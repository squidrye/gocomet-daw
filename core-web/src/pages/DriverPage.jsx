import { useState, useEffect, useRef, useCallback } from 'react'
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import L from 'leaflet'
import { useAuth } from '../context/AuthContext'
import client from '../api/client'
import './DriverPage.css'

const driverIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41],
})

const pickupIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41],
})

const dropoffIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41],
})

const BASE_LAT = 19.076
const BASE_LNG = 72.8777
const SIM_STEPS = 20
const SIM_INTERVAL_MS = 500

function jitter() {
  return (Math.random() - 0.5) * 0.002
}

export default function DriverPage() {
  const { token, logout } = useAuth()
  const [status, setStatus] = useState('OFFLINE')
  const [pos, setPos] = useState({ lat: BASE_LAT, lng: BASE_LNG })
  const [availableRides, setAvailableRides] = useState([])
  const [activeRide, setActiveRide] = useState(null)
  const [tripPhase, setTripPhase] = useState(null) // 'to-pickup' | 'arrived' | 'in-progress' | 'at-dropoff'
  const [error, setError] = useState('')
  const locationRef = useRef(null)
  const sseRef = useRef(null)
  const simRef = useRef(null)

  // Send location updates every 3s when online
  useEffect(() => {
    if (status !== 'OFFLINE') {
      locationRef.current = setInterval(() => {
        setPos((prev) => {
          if (simRef.current) return prev
          const next = { lat: prev.lat + jitter(), lng: prev.lng + jitter() }
          sendLocation(next)
          return next
        })
      }, 3000)
    } else {
      if (locationRef.current) clearInterval(locationRef.current)
    }
    return () => { if (locationRef.current) clearInterval(locationRef.current) }
  }, [status])

  async function sendLocation(loc) {
    try {
      await client.put('/drivers/me/location', { latitude: loc.lat, longitude: loc.lng })
    } catch { /* ignore */ }
  }

  // Connect to ride dispatch SSE when AVAILABLE
  const connectDispatchSSE = useCallback(() => {
    if (sseRef.current) sseRef.current.close()
    const url = `http://localhost:9000/v1/drivers/me/rides/stream?token=${token}`
    const es = new EventSource(url)
    sseRef.current = es

    es.addEventListener('available-rides', (e) => {
      try {
        const rides = JSON.parse(e.data)
        setAvailableRides(Array.isArray(rides) ? rides : [])
      } catch { /* ignore */ }
    })

    es.onerror = () => {
      es.close()
      sseRef.current = null
    }
  }, [token])

  useEffect(() => {
    if (status === 'AVAILABLE' && !activeRide) {
      connectDispatchSSE()
    } else {
      if (sseRef.current) {
        sseRef.current.close()
        sseRef.current = null
      }
      if (status !== 'AVAILABLE') setAvailableRides([])
    }
    return () => {
      if (sseRef.current) { sseRef.current.close(); sseRef.current = null }
    }
  }, [status, activeRide, connectDispatchSSE])

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (sseRef.current) sseRef.current.close()
      if (simRef.current) clearInterval(simRef.current)
    }
  }, [])

  // Simulate driving from current pos to target over SIM_STEPS
  function simulateDrive(target, onComplete) {
    const startLat = pos.lat
    const startLng = pos.lng
    let step = 0
    simRef.current = setInterval(() => {
      step++
      const t = step / SIM_STEPS
      const lat = startLat + (target.lat - startLat) * t
      const lng = startLng + (target.lng - startLng) * t
      const newPos = { lat, lng }
      setPos(newPos)
      sendLocation(newPos)
      if (step >= SIM_STEPS) {
        clearInterval(simRef.current)
        simRef.current = null
        onComplete()
      }
    }, SIM_INTERVAL_MS)
  }

  async function toggleStatus() {
    const next = status === 'OFFLINE' ? 'AVAILABLE' : 'OFFLINE'
    setError('')
    try {
      // Send location before going online so dispatch has it
      if (next === 'AVAILABLE') {
        await sendLocation(pos)
      }
      await client.put('/drivers/me/status', { status: next })
      setStatus(next)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update status')
    }
  }

  async function acceptRide(rideId) {
    setError('')
    try {
      const res = await client.post(`/drivers/me/rides/${rideId}/accept`)
      const ride = availableRides.find(r => r.rideId === rideId)
      setActiveRide({ ...ride, rideId: res.data.rideId || rideId })
      setAvailableRides([])
      setStatus('ON_TRIP')
      setTripPhase('to-pickup')
      // Simulate driving to pickup
      simulateDrive({ lat: ride.pickupLat, lng: ride.pickupLng }, () => {
        setTripPhase('arrived')
      })
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to accept ride')
    }
  }

  async function declineRide(rideId) {
    setError('')
    try {
      await client.post(`/drivers/me/rides/${rideId}/decline`)
      setAvailableRides((prev) => prev.filter(r => r.rideId !== rideId))
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to decline')
    }
  }

  async function startTrip() {
    if (!activeRide) return
    setError('')
    try {
      await client.post(`/trips/${activeRide.rideId}/start`)
      setTripPhase('in-progress')
      // Simulate driving to dropoff
      simulateDrive({ lat: activeRide.dropoffLat, lng: activeRide.dropoffLng }, () => {
        setTripPhase('at-dropoff')
      })
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to start trip')
    }
  }

  async function endTrip() {
    if (!activeRide) return
    setError('')
    try {
      await client.post(`/trips/${activeRide.rideId}/end`)
      setActiveRide(null)
      setTripPhase(null)
      setStatus('AVAILABLE')
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to end trip')
    }
  }

  const isOnline = status !== 'OFFLINE'

  return (
    <div className="driver-layout">
      <div className="driver-map">
        <MapContainer center={[BASE_LAT, BASE_LNG]} zoom={14} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Marker position={[pos.lat, pos.lng]} icon={driverIcon}>
            <Popup>You</Popup>
          </Marker>
          {activeRide && activeRide.pickupLat && (
            <Marker position={[activeRide.pickupLat, activeRide.pickupLng]} icon={pickupIcon}>
              <Popup>Pickup</Popup>
            </Marker>
          )}
          {activeRide && activeRide.dropoffLat && (
            <Marker position={[activeRide.dropoffLat, activeRide.dropoffLng]} icon={dropoffIcon}>
              <Popup>Dropoff</Popup>
            </Marker>
          )}
        </MapContainer>
      </div>

      <div className="driver-panel">
        <div className="panel-header">
          <h2>Driver</h2>
          <button className="btn-text" onClick={logout}>Log out</button>
        </div>

        {error && <p className="panel-error">{error}</p>}

        <div className="status-row">
          <span className="label">Status</span>
          <span className={`badge badge-driver-${status.toLowerCase().replace('_', '-')}`}>
            {status.replace('_', ' ')}
          </span>
        </div>

        {!activeRide && (
          <button
            className={isOnline ? 'btn-danger toggle-btn' : 'btn-primary toggle-btn'}
            onClick={toggleStatus}
          >
            {isOnline ? 'Go Offline' : 'Go Online'}
          </button>
        )}

        {status === 'AVAILABLE' && availableRides.length === 0 && !activeRide && (
          <p className="hint">Waiting for ride requests nearby...</p>
        )}

        {status === 'AVAILABLE' && availableRides.length > 0 && !activeRide && (
          <div className="rides-list">
            <h3>Available Rides ({availableRides.length})</h3>
            {availableRides.map((ride) => (
              <div key={ride.rideId} className="ride-card">
                <div className="ride-detail">
                  <span className="label">Pickup</span>
                  <span className="value mono">{ride.pickupLat?.toFixed(4)}, {ride.pickupLng?.toFixed(4)}</span>
                </div>
                <div className="ride-detail">
                  <span className="label">Dropoff</span>
                  <span className="value mono">{ride.dropoffLat?.toFixed(4)}, {ride.dropoffLng?.toFixed(4)}</span>
                </div>
                {ride.estimatedFare && (
                  <div className="ride-detail">
                    <span className="label">Est. Fare</span>
                    <span className="value">Rs. {Number(ride.estimatedFare).toFixed(2)}</span>
                  </div>
                )}
                <div className="ride-actions">
                  <button className="btn-primary btn-sm" onClick={() => acceptRide(ride.rideId)}>Accept</button>
                  <button className="btn-danger btn-sm" onClick={() => declineRide(ride.rideId)}>Decline</button>
                </div>
              </div>
            ))}
          </div>
        )}

        {activeRide && (
          <div className="trip-card">
            <h3>Active Trip</h3>
            <div className="status-row">
              <span className="label">Phase</span>
              <span className="badge badge-trip">
                {tripPhase === 'to-pickup' && 'Driving to pickup...'}
                {tripPhase === 'arrived' && 'Arrived at pickup'}
                {tripPhase === 'in-progress' && 'Driving to dropoff...'}
                {tripPhase === 'at-dropoff' && 'Arrived at dropoff'}
              </span>
            </div>
            <div className="trip-actions">
              {tripPhase === 'arrived' && (
                <button className="btn-primary" onClick={startTrip}>Start Trip</button>
              )}
              {tripPhase === 'at-dropoff' && (
                <button className="btn-primary" onClick={endTrip}>End Trip</button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
