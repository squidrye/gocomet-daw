import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import './AuthPages.css'

export default function RegisterPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [role, setRole] = useState('RIDER')
  const [vehicleMake, setVehicleMake] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const { register } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const payload = { email, password, role, name }
      if (role === 'DRIVER') payload.vehicleMake = vehicleMake
      const { role: userRole } = await register(payload)
      navigate(userRole === 'RIDER' ? '/rider' : '/driver')
    } catch (err) {
      setError(err.response?.data?.error || 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="auth-form" onSubmit={handleSubmit}>
        <h1>Register</h1>
        {error && <p className="auth-error">{error}</p>}
        <label>
          Name
          <input type="text" value={name} onChange={(e) => setName(e.target.value)} required />
        </label>
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} autoComplete="new-password" />
        </label>
        <fieldset className="role-picker">
          <legend>I am a</legend>
          <button type="button" className={role === 'RIDER' ? 'role-btn active' : 'role-btn'} onClick={() => setRole('RIDER')}>Rider</button>
          <button type="button" className={role === 'DRIVER' ? 'role-btn active' : 'role-btn'} onClick={() => setRole('DRIVER')}>Driver</button>
        </fieldset>
        {role === 'DRIVER' && (
          <label>
            Vehicle (make/model)
            <input type="text" value={vehicleMake} onChange={(e) => setVehicleMake(e.target.value)} required placeholder="e.g. Maruti Swift" />
          </label>
        )}
        <button type="submit" className="btn-primary" disabled={loading}>
          {loading ? 'Creating account...' : 'Register'}
        </button>
        <p className="auth-link">
          Already have an account? <Link to="/login">Log in</Link>
        </p>
      </form>
    </div>
  )
}
