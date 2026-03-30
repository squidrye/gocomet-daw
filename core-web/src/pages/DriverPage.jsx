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

// Simulated base location (Mumbai)
const BASE_LAT = 19.076
const BASE_LNG = 72.8777

function jitter() {
  return (Math.random() - 0.5) * 0.002
}

export default function DriverPage() {
  const { logout } = useAuth()
  const [status, setStatus] = useState('OFFLINE')
  const [pos, setPos] = useState({ lat: BASE_LAT, lng: BASE_LNG })
  const [offer, setOffer] = useState(null)
  const [activeRide, setActiveRide] = useState(null)
  const [error, setError] = useState('')
  const locationRef = useRef(null)
  const offerPollRef = useRef(null)

  // Send location updates every 3s when online
  useEffect(() => {
    if (status !== 'OFFLINE') {
      sendLocation(pos)
      locationRef.current = setInterval(() => {
        setPos((prev) => {
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

  // Poll for offers every 2s when AVAILABLE or LOCKED
  useEffect(() => {
    if ((status === 'AVAILABLE' || status === 'LOCKED') && !offer && !activeRide) {
      offerPollRef.current = setInterval(async () => {
        try {
          const res = await client.get('/drivers/me/offer')
          if (res.status === 200 && res.data && res.data.rideId) {
            setOffer(res.data)
            setStatus('LOCKED')
          }
        } catch { /* 204 or error, ignore */ }
      }, 2000)
    } else {
      if (offerPollRef.current) clearInterval(offerPollRef.current)
    }
    return () => { if (offerPollRef.current) clearInterval(offerPollRef.current) }
  }, [status, offer, activeRide])

  async function toggleStatus() {
    const next = status === 'OFFLINE' ? 'AVAILABLE' : 'OFFLINE'
    setError('')
    try {
      await client.put('/drivers/me/status', { status: next })
      setStatus(next)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to update status')
    }
  }

  async function acceptOffer() {
    setError('')
    try {
      await client.post('/drivers/me/offer/accept')
      setActiveRide({ ...offer, tripStatus: 'ACCEPTED' })
      setOffer(null)
      setStatus('ON_TRIP')
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to accept')
    }
  }

  async function declineOffer() {
    setError('')
    try {
      await client.post('/drivers/me/offer/decline')
      setOffer(null)
      setStatus('AVAILABLE')
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to decline')
    }
  }

  async function startTrip() {
    if (!activeRide) return
    setError('')
    try {
      await client.post(`/trips/${activeRide.rideId}/start`)
      setActiveRide((prev) => prev ? { ...prev, tripStatus: 'IN_PROGRESS' } : prev)
    } catch (err) {
      setError(err.response?.data?.error || 'Failed to start trip')
    }
  }

  async function endTrip() {
    if (!activeRide) return
    setError('')
    try {
      const res = await client.post(`/trips/${activeRide.rideId}/end`)
      setActiveRide(null)
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
          {(offer || activeRide) && (offer || activeRide).pickupLat && (
            <Marker
              position={[(offer || activeRide).pickupLat, (offer || activeRide).pickupLng]}
              icon={pickupIcon}
            >
              <Popup>Pickup</Popup>
            </Marker>
          )}
          {(offer || activeRide) && (offer || activeRide).dropoffLat && (
            <Marker
              position={[(offer || activeRide).dropoffLat, (offer || activeRide).dropoffLng]}
              icon={dropoffIcon}
            >
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
          <span className={`badge badge-driver-${status.toLowerCase()}`}>{status.replace('_', ' ')}</span>
        </div>

        {!activeRide && !offer && (
          <button
            className={isOnline ? 'btn-danger toggle-btn' : 'btn-primary toggle-btn'}
            onClick={toggleStatus}
          >
            {isOnline ? 'Go Offline' : 'Go Online'}
          </button>
        )}

        {offer && (
          <div className="offer-card">
            <h3>New Ride Offer</h3>
            <div className="offer-detail">
              <span className="label">Pickup</span>
              <span className="value mono">{offer.pickupLat?.toFixed(4)}, {offer.pickupLng?.toFixed(4)}</span>
            </div>
            <div className="offer-detail">
              <span className="label">Dropoff</span>
              <span className="value mono">{offer.dropoffLat?.toFixed(4)}, {offer.dropoffLng?.toFixed(4)}</span>
            </div>
            {offer.estimatedFare && (
              <div className="offer-detail">
                <span className="label">Est. Fare</span>
                <span className="value">Rs. {Number(offer.estimatedFare).toFixed(2)}</span>
              </div>
            )}
            <div className="offer-actions">
              <button className="btn-primary" onClick={acceptOffer}>Accept</button>
              <button className="btn-danger" onClick={declineOffer}>Decline</button>
            </div>
          </div>
        )}

        {activeRide && (
          <div className="trip-card">
            <h3>Active Trip</h3>
            <div className="status-row">
              <span className="label">Trip Status</span>
              <span className={`badge badge-${activeRide.tripStatus.toLowerCase()}`}>
                {activeRide.tripStatus.replace('_', ' ')}
              </span>
            </div>
            <div className="trip-actions">
              {activeRide.tripStatus === 'ACCEPTED' && (
                <button className="btn-primary" onClick={startTrip}>Start Trip</button>
              )}
              {activeRide.tripStatus === 'IN_PROGRESS' && (
                <button className="btn-primary" onClick={endTrip}>End Trip</button>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
