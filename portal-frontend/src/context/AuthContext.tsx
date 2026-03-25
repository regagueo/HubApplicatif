import { createContext, useContext, useState, ReactNode } from 'react'

export interface User {
  id: number
  username: string
  email: string
  firstName?: string
  lastName?: string
  mfaEnabled?: boolean
  roles: string[]
}

interface AuthContextType {
  token: string | null
  user: User | null
  pendingMfa: { tempToken: string; user: User } | null
  login: (username: string, password: string) => Promise<{ requiresMfa?: boolean }>
  verifyMfa: (code: string) => Promise<void>
  resendOtp: () => Promise<void>
  clearPendingMfa: () => void
  loginWithAccessToken: (accessToken: string) => Promise<User>
  updateUser: (updates: Partial<User>) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextType | null>(null)

const STORAGE_KEY = 'portail_auth'
const TOKEN_KEY = STORAGE_KEY + '_token'
const USER_KEY = STORAGE_KEY + '_user'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => 
    localStorage.getItem(TOKEN_KEY))
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem(USER_KEY)
    return saved ? JSON.parse(saved) : null
  })
  const [pendingMfa, setPendingMfa] = useState<{ tempToken: string; user: User } | null>(null)
  const apiBase = import.meta.env.DEV ? '/api/auth' : (import.meta.env.VITE_API_URL || window.location.origin) + '/auth'

  const login = async (usernameOrEmail: string, password: string): Promise<{ requiresMfa?: boolean }> => {
    let res: Response
    try {
      res = await fetch(`${apiBase}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ usernameOrEmail, password })
      })
    } catch (err) {
      throw new Error('Impossible de contacter le serveur. Vérifiez que l\'auth-service est démarré (port 8081).')
    }
    const text = await res.text()
    let data: { error?: string; accessToken?: string; user?: User; requiresMfa?: boolean; tempToken?: string }
    try {
      data = text ? JSON.parse(text) : {}
    } catch {
      throw new Error('Réponse invalide du serveur. Vérifiez que l\'auth-service est démarré.')
    }
    if (!res.ok) throw new Error(data.error || 'Identifiants incorrects')
    if (data.requiresMfa && data.tempToken && data.user) {
      setPendingMfa({ tempToken: data.tempToken, user: data.user })
      return { requiresMfa: true }
    }
    if (!data.accessToken || !data.user) {
      throw new Error('Réponse incomplète du serveur')
    }
    setToken(data.accessToken)
    setUser(data.user)
    localStorage.setItem(TOKEN_KEY, data.accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
    return {}
  }

  const verifyMfa = async (code: string): Promise<void> => {
    if (!pendingMfa) throw new Error('Session MFA expirée')
    const res = await fetch(`${apiBase}/mfa/verify`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tempToken: pendingMfa.tempToken, code })
    })
    const text = await res.text()
    let data: { error?: string; accessToken?: string; user?: User }
    try { data = text ? JSON.parse(text) : {} } catch { throw new Error('Réponse invalide') }
    if (!res.ok) throw new Error(data.error || 'Code invalide')
    if (!data.accessToken || !data.user) throw new Error('Réponse incomplète')
    setPendingMfa(null)
    setToken(data.accessToken)
    setUser(data.user)
    localStorage.setItem(TOKEN_KEY, data.accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(data.user))
  }

  const resendOtp = async (): Promise<void> => {
    if (!pendingMfa) throw new Error('Session MFA expirée')
    const res = await fetch(`${apiBase}/mfa/resend-otp`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ tempToken: pendingMfa.tempToken })
    })
    if (!res.ok) {
      const text = await res.text()
      let data: { error?: string } = {}
      try { data = text ? JSON.parse(text) : {} } catch { }
      throw new Error(data?.error || 'Impossible de renvoyer le code')
    }
  }

  const clearPendingMfa = () => setPendingMfa(null)

  const loginWithAccessToken = async (accessToken: string): Promise<User> => {
    const res = await fetch(`${apiBase}/me`, {
      headers: {
        Authorization: `Bearer ${accessToken}`
      }
    })
    const text = await res.text()
    let data: User | { error?: string } = {} as User
    try { data = text ? JSON.parse(text) : {} as User } catch { throw new Error('Réponse invalide du serveur') }
    if (!res.ok) throw new Error((data as { error?: string }).error || 'Impossible de charger le profil utilisateur')
    const profile = data as User
    setToken(accessToken)
    setUser(profile)
    localStorage.setItem(TOKEN_KEY, accessToken)
    localStorage.setItem(USER_KEY, JSON.stringify(profile))
    return profile
  }

  const updateUser = (updates: Partial<User>) => {
    if (user) {
      const updated = { ...user, ...updates }
      setUser(updated)
      localStorage.setItem(USER_KEY, JSON.stringify(updated))
    }
  }

  const logout = () => {
    setToken(null)
    setUser(null)
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
  }

  return (
    <AuthContext.Provider value={{ token, user, pendingMfa, login, verifyMfa, resendOtp, clearPendingMfa, loginWithAccessToken, updateUser, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
