const API_BASE = import.meta.env.DEV ? '/api' : (import.meta.env.VITE_API_URL || 'http://localhost:8080')

export async function apiFetch(path: string, options: RequestInit = {}) {
  const token = localStorage.getItem('portail_auth_token')
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
    ...options.headers
  }
  if (token) {
    (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`
  }
  const res = await fetch(`${API_BASE}${path}`, { ...options, headers })
  if (res.status === 401) {
    localStorage.removeItem('portail_auth_token')
    localStorage.removeItem('portail_auth_user')
    window.location.href = '/login'
  }
  return res
}
