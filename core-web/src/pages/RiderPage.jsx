import { useState, useEffect, useRef, useCallback } from 'react'
import { MapContainer, TileLayer, Marker, Popup, Circle, useMapEvents } from 'react-leaflet'
import { useAuth } from '../context/AuthContext'
import client, { API_BASE } from '../api/client'
import './RiderPage.css'

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

  const isCancellable = ride && ['REQUESTED', 'ACCEPTED'].includes(ride.status)
  const showPay = ride && ride.status === 'COMPLETED' && !ride.paid

  function handleMapClick(latlng) {
    if (ride) return
    if (!pickup) setPickup(latlng)
    else if (!dropoff) setDropoff(latlng)
  }

  function resetMarkers() {
    setPickup(null)
    setDropoff(null)
  }

  const connectSSE = useCallback((rideId) => {
    if (sseRef.current) sseRef.current.close()
    const es = new EventSource(`${API_BASE}/rides/${rideId}/events?token=${token}`)
    sseRef.current = es
    es.addEventListener('ride-update', (e) => {
      try {
        const data = JSON.parse(e.data)
        setRide((prev) => {
          if (!prev) return prev
          const merged = { ...prev }
          Object.keys(data).forEach((key) => {
            if (data[key] != null) merged[key] = data[key]
          })
          return merged
        })
      } catch {}
    })
    es.onerror = () => { es.close(); sseRef.current = null }
  }, [token])

  useEffect(() => {
    async function fetchActiveRide() {
      try {
        const res = await client.get('/rides/active')
        if (res.status === 200 && res.data) {
          const r = { ...res.data, paid: false }
          setRide(r)
          setPickup({ lat: r.pickupLat, lng: r.pickupLng })
          setDropoff({ lat: r.dropoffLat, lng: r.dropoffLng })
          if (r.status !== 'COMPLETED' && r.status !== 'CANCELLED') connectSSE(r.id)
        }
      } catch {}
    }
    fetchActiveRide()
  }, [connectSSE])

  useEffect(() => {
    if (ride && (ride.status === 'ACCEPTED' || ride.status === 'IN_PROGRESS')) {
      pollRef.current = setInterval(async () => {
        try {
          const res = await client.get(`/rides/${ride.id}/driver-location`)
          setDriverPos({ lat: res.data.latitude, lng: res.data.longitude })
        } catch {}
      }, 3000)
    } else {
      setDriverPos(null)
      if (pollRef.current) clearInterval(pollRef.current)
    }
    return () => { if (pollRef.current) clearInterval(pollRef.current) }
  }, [ride?.status, ride?.id])

  useEffect(() => () => { if (sseRef.current) sseRef.current.close() }, [])

  async function requestRide() {
    if (!pickup || !dropoff) return
    setError('')
    setLoading(true)
    try {
      const res = await client.post('/rides', {
        pickupLat: pickup.lat, pickupLng: pickup.lng,
        dropoffLat: dropoff.lat, dropoffLng: dropoff.lng,
      })
      const r = { ...res.data, paid: false }
      setRide(r)
      connectSSE(r.id)
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

  const searchRadiusM = ride?.status === 'REQUESTED' ? ((ride.searchRadiusKm || 5) * 1000) : null

  return (
    <div className="rider-layout">
      <div className="rider-map">
        <MapContainer center={[19.076, 72.8777]} zoom={13} style={{ height: '100%', width: '100%' }}>
          <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
          <MapClickHandler onMapClick={handleMapClick} />
          {pickup && <Marker position={pickup}><Popup>Pickup</Popup></Marker>}
          {dropoff && <Marker position={dropoff}><Popup>Dropoff</Popup></Marker>}
          {driverPos && <Marker position={driverPos}><Popup>Driver</Popup></Marker>}
          {pickup && searchRadiusM && (
            <Circle center={pickup} radius={searchRadiusM} pathOptions={{ color: '#2563eb', fillOpacity: 0.08, weight: 1 }} />
          )}
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
            {pickup && <p className="coord">Pickup: {pickup.lat.toFixed(4)}, {pickup.lng.toFixed(4)}</p>}
            {dropoff && <p className="coord">Dropoff: {dropoff.lat.toFixed(4)}, {dropoff.lng.toFixed(4)}</p>}
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
              <span className={`badge badge-${(ride.status || '').toLowerCase()}`}>{(ride.status || '').replace('_', ' ')}</span>
            </div>
            {ride.surgeMultiplier > 1 && (
              <div className="status-row">
                <span className="label">Surge</span>
                <span className="value surge">{ride.surgeMultiplier}x</span>
              </div>
            )}
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
            {ride.driverName && (
              <div className="status-row">
                <span className="label">Driver</span>
                <span className="value">{ride.driverName}</span>
              </div>
            )}
            {ride.driverVehicle && (
              <div className="status-row">
                <span className="label">Vehicle</span>
                <span className="value">{ride.driverVehicle}</span>
              </div>
            )}
            {ride.status === 'REQUESTED' && <p className="hint">Looking for drivers nearby...</p>}

            <div className="panel-actions">
              {isCancellable && <button className="btn-danger" onClick={cancelRide}>Cancel Ride</button>}
              {showPay && <button className="btn-primary" onClick={payRide}>Pay Rs. {Number(ride.finalFare).toFixed(2)}</button>}
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
