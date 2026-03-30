import { useState, useEffect, useRef, useCallback } from 'react'
import { MapContainer, TileLayer, Marker, Popup, useMapEvents } from 'react-leaflet'
import L from 'leaflet'
import { useAuth } from '../context/AuthContext'
import client from '../api/client'
import './RiderPage.css'

const greenIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-green.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41],
})

const redIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41],
})

const blueIcon = new L.Icon({
  iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-blue.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
  iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41],
})

function MapClickHandler({ onMapClick }) {
  useMapEvents({ click: (e) => onMapClick(e.latlng) })
  return null
}

export default function RiderPage() {
  const { token, logout } = useAuth()
  const [pickup, setPickup] = useState(null)
  const [dropoff, setDropoff] = useState(null)
  const [ride, setRide] = useState(null)
  const [driverPos, setDriverPos] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const sseRef = useRef(null)
  const pollRef = useRef(null)

  const isCancellable = ride && ['REQUESTED', 'MATCHED', 'ACCEPTED'].includes(ride.status)
  const showPay = ride && ride.status === 'COMPLETED' && !ride.paid

  function handleMapClick(latlng) {
    if (ride) return
    if (!pickup) {
      setPickup(latlng)
    } else if (!dropoff) {
      setDropoff(latlng)
    }
  }

  function resetMarkers() {
    setPickup(null)
    setDropoff(null)
  }

  const connectSSE = useCallback((rideId) => {
    if (sseRef.current) sseRef.current.close()
    const url = `http://localhost:9000/v1/rides/${rideId}/events?token=${token}`
    const es = new EventSource(url)
    sseRef.current = es

    es.addEventListener('ride-update', (e) => {
      try {
        const data = JSON.parse(e.data)
        setRide((prev) => prev ? { ...prev, ...data } : prev)
      } catch { /* ignore parse errors */ }
    })

    es.onerror = () => {
      es.close()
      sseRef.current = null
    }
  }, [token])

  // Fetch active ride on mount
  useEffect(() => {
    async function fetchActiveRide() {
      try {
        const res = await client.get('/rides/active')
        if (res.status === 200 && res.data) {
          const activeRide = { ...res.data, paid: false }
          setRide(activeRide)
          setPickup({ lat: activeRide.pickupLat, lng: activeRide.pickupLng })
          setDropoff({ lat: activeRide.dropoffLat, lng: activeRide.dropoffLng })
          connectSSE(activeRide.id)
        }
      } catch { /* no active ride, that's fine */ }
    }
    fetchActiveRide()
  }, [connectSSE])

  // Poll driver location when ride is ACCEPTED or IN_PROGRESS
  useEffect(() => {
    if (ride && (ride.status === 'ACCEPTED' || ride.status === 'IN_PROGRESS')) {
      pollRef.current = setInterval(async () => {
        try {
          const res = await client.get(`/rides/${ride.id}/driver-location`)
          setDriverPos({ lat: res.data.latitude, lng: res.data.longitude })
        } catch { /* ignore */ }
      }, 3000)
    } else {
      setDriverPos(null)
      if (pollRef.current) clearInterval(pollRef.current)
    }
    return () => { if (pollRef.current) clearInterval(pollRef.current) }
  }, [ride?.status, ride?.id])

  // Cleanup SSE on unmount
  useEffect(() => {
    return () => {
      if (sseRef.current) sseRef.current.close()
    }
  }, [])

  async function requestRide() {
    if (!pickup || !dropoff) return
    setError('')
    setLoading(true)
    try {
      const res = await client.post('/rides', {
        pickupLat: pickup.lat,
        pickupLng: pickup.lng,
        dropoffLat: dropoff.lat,
        dropoffLng: dropoff.lng,
      })
      const newRide = { ...res.data, paid: false }
      setRide(newRide)
      connectSSE(newRide.id)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to request ride')
    } finally {
      setLoading(false)
    }
  }

  async function cancelRide() {
    if (!ride) return
    try {
      await client.post(`/rides/${ride.id}/cancel`)
      setRide((prev) => prev ? { ...prev, status: 'CANCELLED' } : prev)
      if (sseRef.current) sseRef.current.close()
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to cancel')
    }
  }

  async function payRide() {
    if (!ride) return
    try {
      await client.post('/payments', { rideId: ride.id })
      setRide((prev) => prev ? { ...prev, paid: true } : prev)
    } catch (err) {
      setError(err.response?.data?.error || 'Payment failed')
    }
  }

  function newRide() {
    setRide(null)
    setPickup(null)
    setDropoff(null)
    setDriverPos(null)
    setError('')
    if (sseRef.current) sseRef.current.close()
  }

  return (
    <div className="rider-layout">
      <div className="rider-map">
        <MapContainer center={[19.076, 72.8777]} zoom={13} style={{ height: '100%', width: '100%' }}>
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <MapClickHandler onMapClick={handleMapClick} />
          {pickup && <Marker position={pickup} icon={greenIcon}><Popup>Pickup</Popup></Marker>}
          {dropoff && <Marker position={dropoff} icon={redIcon}><Popup>Dropoff</Popup></Marker>}
          {driverPos && <Marker position={driverPos} icon={blueIcon}><Popup>Driver</Popup></Marker>}
        </MapContainer>
      </div>

      <div className="rider-panel">
        <div className="panel-header">
          <h2>Ride</h2>
          <button className="btn-text" onClick={logout}>Log out</button>
        </div>

        {error && <p className="panel-error">{error}</p>}

        {!ride && (
          <div className="panel-section">
            <p className="hint">
              {!pickup ? 'Click the map to set pickup' : !dropoff ? 'Click the map to set dropoff' : 'Ready to request'}
            </p>
            {pickup && (
              <p className="coord">Pickup: {pickup.lat.toFixed(4)}, {pickup.lng.toFixed(4)}</p>
            )}
            {dropoff && (
              <p className="coord">Dropoff: {dropoff.lat.toFixed(4)}, {dropoff.lng.toFixed(4)}</p>
            )}
            <div className="panel-actions">
              {pickup && <button className="btn-secondary" onClick={resetMarkers}>Reset</button>}
              {pickup && dropoff && (
                <button className="btn-primary" onClick={requestRide} disabled={loading}>
                  {loading ? 'Requesting...' : 'Request Ride'}
                </button>
              )}
            </div>
          </div>
        )}

        {ride && (
          <div className="panel-section">
            <div className="status-row">
              <span className="label">Status</span>
              <span className={`badge badge-${ride.status.toLowerCase()}`}>{ride.status}</span>
            </div>
            {ride.estimatedFare && (
              <div className="status-row">
                <span className="label">Est. Fare</span>
                <span className="value">Rs. {Number(ride.estimatedFare).toFixed(2)}</span>
              </div>
            )}
            {ride.finalFare && (
              <div className="status-row">
                <span className="label">Final Fare</span>
                <span className="value">Rs. {Number(ride.finalFare).toFixed(2)}</span>
              </div>
            )}
            {ride.driverId && (
              <div className="status-row">
                <span className="label">Driver</span>
                <span className="value mono">{ride.driverId.slice(0, 8)}...</span>
              </div>
            )}
            {ride.message && <p className="ride-message">{ride.message}</p>}

            <div className="panel-actions">
              {isCancellable && (
                <button className="btn-danger" onClick={cancelRide}>Cancel Ride</button>
              )}
              {showPay && (
                <button className="btn-primary" onClick={payRide}>Pay</button>
              )}
              {ride.paid && <p className="paid-label">Payment complete</p>}
              {(ride.status === 'COMPLETED' && ride.paid) || ride.status === 'CANCELLED' ? (
                <button className="btn-secondary" onClick={newRide}>New Ride</button>
              ) : null}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
