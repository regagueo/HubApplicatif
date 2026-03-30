import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Eye, EyeOff } from 'lucide-react'
import { useAuth, type User } from '../context/AuthContext'
import loginBg from '../assets/image.jpeg'
import companyLogo from '../assets/logo.png'

export default function Login() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [mfaCode, setMfaCode] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [resending, setResending] = useState(false)
  const { login, verifyMfa, resendOtp, pendingMfa, clearPendingMfa, loginWithAccessToken } = useAuth()
  const navigate = useNavigate()

  const hasRole = (profile: User, role: string): boolean => {
    return (profile.roles || []).some((r) => (r.startsWith('ROLE_') ? r.slice(5) : r) === role)
  }

  useEffect(() => {
    const params = new URLSearchParams(window.location.search)
    const ssoToken = params.get('ssoToken')
    const ssoError = params.get('ssoError')
    if (ssoError) {
      setError(`SSO Microsoft: ${ssoError}`)
      return
    }
    if (!ssoToken) return

    setLoading(true)
    loginWithAccessToken(ssoToken)
      .then((profile) => {
        window.history.replaceState({}, document.title, window.location.pathname)
        if (hasRole(profile, 'RH')) {
          navigate('/home')
          return
        }
        if (hasRole(profile, 'MANAGER')) {
          navigate('/home')
          return
        }
        navigate('/home')
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Erreur SSO Microsoft'))
      .finally(() => setLoading(false))
  }, [loginWithAccessToken, navigate])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const result = await login(username, password)
      if (!result?.requiresMfa) navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur de connexion')
    } finally {
      setLoading(false)
    }
  }

  const handleVerifyMfa = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await verifyMfa(mfaCode)
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Code invalide')
    } finally {
      setLoading(false)
    }
  }

  const handleResendOtp = async () => {
    setError('')
    setResending(true)
    try {
      await resendOtp()
      setMfaCode('')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur')
    } finally {
      setResending(false)
    }
  }

  if (pendingMfa) {
    return (
      <div style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundImage: `linear-gradient(rgba(0,0,0,0.45), rgba(0,0,0,0.45)), url(${loginBg})`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat'
      }}>
        <div style={{ background: 'var(--bg-card)', padding: '2.5rem', borderRadius: 'var(--radius-lg)', boxShadow: '0 4px 20px rgba(0,0,0,0.08)', width: '100%', maxWidth: '400px' }}>
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
            <img src={companyLogo} alt="Logo CBI" style={{ width: '150px', height: 'auto' }} />
          </div>
          <h1 style={{ textAlign: 'center', marginBottom: '1.5rem', fontSize: '1.75rem' }}>Vérification en deux étapes</h1>
          <p style={{ textAlign: 'center', color: 'var(--text-secondary)', marginBottom: '1rem' }}>Un code à 6 chiffres a été envoyé à <strong>{pendingMfa.user.email}</strong></p>
          <p style={{ textAlign: 'center', color: 'var(--text-secondary)', marginBottom: '2rem', fontSize: '0.8rem' }}>Consultez Mailtrap ou votre boîte mail. Max 5 tentatives, expiration 5 min.</p>
          <form onSubmit={handleVerifyMfa}>
            <div style={{ marginBottom: '1.25rem' }}>
              <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem' }}>Code à 6 chiffres</label>
              <input type="text" inputMode="numeric" maxLength={6} value={mfaCode} onChange={e => setMfaCode(e.target.value.replace(/\D/g, ''))} placeholder="000000" required autoFocus
                style={{ width: '100%', padding: '0.75rem 1rem', borderRadius: '8px', border: '1px solid var(--border)', background: 'var(--bg-primary)', color: 'var(--text-primary)', fontSize: '1.25rem', letterSpacing: '0.5em', textAlign: 'center' }} />
            </div>
            {error && <p style={{ color: 'var(--error)', marginBottom: '1rem', fontSize: '0.875rem' }}>{error}</p>}
            <button type="submit" disabled={loading || mfaCode.length !== 6} style={{ width: '100%', padding: '0.75rem', borderRadius: '8px', border: 'none', background: 'var(--accent)', color: 'white', fontSize: '1rem', cursor: loading ? 'not-allowed' : 'pointer' }}>
              {loading ? 'Vérification...' : 'Valider'}
            </button>
            <button type="button" onClick={handleResendOtp} disabled={resending} style={{ width: '100%', marginTop: '0.75rem', padding: '0.5rem', background: 'transparent', border: 'none', color: 'var(--text-secondary)', fontSize: '0.875rem', cursor: resending ? 'not-allowed' : 'pointer', textDecoration: 'underline' }}>
              {resending ? 'Envoi...' : 'Renvoyer le code'}
            </button>
            <button type="button" onClick={() => { clearPendingMfa(); setMfaCode(''); setError('') }} style={{ width: '100%', marginTop: '0.5rem', padding: '0.5rem', background: 'transparent', border: 'none', color: 'var(--text-secondary)', fontSize: '0.8rem', cursor: 'pointer' }}>
              ← Retour à la connexion
            </button>
          </form>
        </div>
      </div>
    )
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundImage: `linear-gradient(rgba(0,0,0,0.45), rgba(0,0,0,0.45)), url(${loginBg})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    }}>
      <div style={{
        background: 'var(--bg-card)',
        boxShadow: '0 4px 20px rgba(0,0,0,0.08)',
        padding: '2.5rem',
        borderRadius: 'var(--radius-lg)',
        width: '100%',
        maxWidth: '400px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '1rem' }}>
          <img src={companyLogo} alt="Logo CBI" style={{ width: '150px', height: 'auto' }} />
        </div>
        <h1 style={{ textAlign: 'center', marginBottom: '1.5rem', fontSize: '1.75rem' }}>
          Portail Intranet
        </h1>
        <p style={{ textAlign: 'center', color: 'var(--text-secondary)', marginBottom: '2rem' }}>
          Connexion au portail
        </p>

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem' }}>
              Email ou nom d'utilisateur
            </label>
            <input
              type="text"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
              style={{
                width: '100%',
                padding: '0.75rem 1rem',
                borderRadius: '8px',
                border: '1px solid var(--border)',
                background: 'var(--bg-primary)',
                color: 'var(--text-primary)'
              }}
            />
          </div>
          <div style={{ marginBottom: '1.5rem' }}>
            <label style={{ display: 'block', marginBottom: '0.5rem', fontSize: '0.875rem' }}>
              Mot de passe
            </label>
            <div style={{ position: 'relative' }}>
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={e => setPassword(e.target.value)}
                required
                style={{
                  width: '100%',
                  padding: '0.75rem 2.75rem 0.75rem 1rem',
                  borderRadius: '8px',
                  border: '1px solid var(--border)',
                  background: 'var(--bg-primary)',
                  color: 'var(--text-primary)'
                }}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? 'Masquer le mot de passe' : 'Afficher le mot de passe'}
                style={{
                  position: 'absolute',
                  right: '0.75rem',
                  top: '50%',
                  transform: 'translateY(-50%)',
                  background: 'none',
                  border: 'none',
                  cursor: 'pointer',
                  color: 'var(--text-secondary)',
                  padding: '0.25rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
          </div>
          {error && (
            <p style={{ color: 'var(--error)', marginBottom: '1rem', fontSize: '0.875rem' }}>
              {error}
            </p>
          )}
          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '0.75rem',
              borderRadius: '8px',
              border: 'none',
              background: 'var(--accent)',
              color: 'white',
              fontSize: '1rem',
              cursor: loading ? 'not-allowed' : 'pointer'
            }}
          >
            {loading ? 'Connexion...' : 'Se connecter'}
          </button>
          <button
            type="button"
            onClick={() => {
              const authBase = import.meta.env.DEV
                ? 'http://localhost:8081'
                : (import.meta.env.VITE_API_URL || window.location.origin)
              window.location.href = `${authBase}/oauth2/authorization/azure`
            }}
            style={{
              width: '100%',
              marginTop: '0.75rem',
              padding: '0.75rem',
              borderRadius: '8px',
              border: '1px solid var(--border)',
              background: 'white',
              color: '#111827',
              fontSize: '1rem',
              cursor: 'pointer'
            }}
          >
            Se connecter avec Azure
          </button>
        </form>
      </div>
    </div>
  )
}
