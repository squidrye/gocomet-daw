import { createContext, useContext, useState } from 'react'
import client from '../api/client'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem('token'))
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('user')
      return stored ? JSON.parse(stored) : null
    } catch { return null }
  })

  async function login(email, password) {
    const res = await client.post('/auth/login', { email, password })
    const { token: jwt, userId, role } = res.data
    localStorage.setItem('token', jwt)
    localStorage.setItem('user', JSON.stringify({ userId, role }))
    setToken(jwt)
    setUser({ userId, role })
    return { userId, role }
  }

  async function register(email, password, role) {
    const res = await client.post('/auth/register', { email, password, role })
    const { token: jwt, userId, role: userRole } = res.data
    localStorage.setItem('token', jwt)
    localStorage.setItem('user', JSON.stringify({ userId, role: userRole }))
    setToken(jwt)
    setUser({ userId, role: userRole })
    return { userId, role: userRole }
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setToken(null)
    setUser(null)
  }

  const isAuthenticated = !!token && !!user

  return (
    <AuthContext.Provider value={{ user, token, login, register, logout, isAuthenticated }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
