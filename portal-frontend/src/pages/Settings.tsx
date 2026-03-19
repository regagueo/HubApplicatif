import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../context/AuthContext'
import { User, Palette, Shield, Bell, Save, Sun, Moon, CheckCircle } from 'lucide-react'
import * as api from '../api/parametres'
import type { ProfilDto, PreferencesDto, SecuriteDto, NotificationsDto } from '../api/parametres'
import './Settings.css'

const AUTH_API = import.meta.env.DEV ? '/api/auth' : (import.meta.env.VITE_API_URL || window.location.origin) + '/auth'

type TabId = 'profil' | 'preferences' | 'securite' | 'notifications'

function hasRole(roles: string[] | undefined, role: string): boolean {
  if (!roles) return false
  return roles.some((r) => (r.startsWith('ROLE_') ? r.slice(5) : r) === role)
}

export default function Settings() {
  const { user, token, updateUser } = useAuth()
  const [activeTab, setActiveTab] = useState<TabId>('profil')
  const [profil, setProfil] = useState<ProfilDto>({})
  const [preferences, setPreferences] = useState<PreferencesDto>({ language: 'fr', theme: 'light' })
  const [securite, setSecurite] = useState<SecuriteDto>({})
  const [notifications, setNotifications] = useState<NotificationsDto>({ emailAlerts: true, pushEnabled: false, smsEnabled: false })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saveSuccess, setSaveSuccess] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [mfaLoading, setMfaLoading] = useState(false)
  const [mfaError, setMfaError] = useState('')

  const isRhOrAdmin = hasRole(user?.roles, 'RH') || hasRole(user?.roles, 'ADMIN')

  const load = useCallback(async () => {
    if (!token) return
    setLoading(true)
    setError(null)
    try {
      const [p, pref, sec, notif] = await Promise.all([
        api.getProfil().catch(() => ({})),
        api.getPreferences().catch(() => ({ language: 'fr', theme: 'light' })),
        api.getSecurite().catch(() => ({})),
        api.getNotifications().catch(() => ({ emailAlerts: true, pushEnabled: false, smsEnabled: false }))
      ])
      setProfil(p)
      setPreferences(pref)
      setSecurite(sec)
      setNotifications(notif)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur chargement')
    } finally {
      setLoading(false)
    }
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', preferences.theme === 'dark' ? 'dark' : 'light')
  }, [preferences.theme])

  const authHeaders = () => ({
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {})
  })

  const handleSaveProfil = async () => {
    setSaving(true)
    setError(null)
    try {
      const updated = await api.updateProfil(profil)
      setProfil(updated)
      if (updated.emailPro && user) updateUser({ email: updated.emailPro })
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 3000)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur enregistrement')
    } finally {
      setSaving(false)
    }
  }

  const handleSavePreferences = async () => {
    setSaving(true)
    setError(null)
    try {
      const updated = await api.updatePreferences(preferences)
      setPreferences(updated)
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 3000)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur enregistrement')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveSecurite = async () => {
    setSaving(true)
    setError(null)
    try {
      const updated = await api.updateSecurite(securite)
      setSecurite(updated)
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 3000)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur enregistrement')
    } finally {
      setSaving(false)
    }
  }

  const handleSaveNotifications = async () => {
    setSaving(true)
    setError(null)
    try {
      const updated = await api.updateNotifications(notifications)
      setNotifications(updated)
      setSaveSuccess(true)
      setTimeout(() => setSaveSuccess(false), 3000)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur enregistrement')
    } finally {
      setSaving(false)
    }
  }

  const handleEnableMfa = async () => {
    setMfaError('')
    setMfaLoading(true)
    try {
      const res = await fetch(`${AUTH_API}/mfa/enable-email`, { method: 'POST', headers: authHeaders() })
      const text = await res.text()
      let data: { error?: string } = {}
      try { data = text ? JSON.parse(text) : {} } catch { /* */ }
      if (!res.ok) throw new Error(data.error || (res.status === 401 ? 'Session expirée.' : `Erreur ${res.status}`))
      updateUser({ mfaEnabled: true })
      setSecurite((s) => ({ ...s, mfaEnabled: true }))
      await api.updateSecurite({ mfaEnabled: true })
    } catch (err) {
      setMfaError(err instanceof Error ? err.message : 'Erreur')
    } finally {
      setMfaLoading(false)
    }
  }

  const handleDisableMfa = async () => {
    setMfaError('')
    setMfaLoading(true)
    try {
      const res = await fetch(`${AUTH_API}/mfa/disable`, { method: 'POST', headers: authHeaders() })
      const text = await res.text()
      let data: { error?: string } = {}
      try { data = text ? JSON.parse(text) : {} } catch { }
      if (!res.ok) throw new Error(data.error || (res.status === 401 ? 'Session expirée.' : `Erreur ${res.status}`))
      updateUser({ mfaEnabled: false })
      setSecurite((s) => ({ ...s, mfaEnabled: false }))
      await api.updateSecurite({ mfaEnabled: false })
    } catch (err) {
      setMfaError(err instanceof Error ? err.message : 'Erreur')
    } finally {
      setMfaLoading(false)
    }
  }

  const displayName = user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : user?.username ?? ''
  const displayEmail = (profil.emailPro || user?.email) ?? ''
  const mfaOn = securite.mfaEnabled ?? user?.mfaEnabled ?? false
  const passwordChangedAt = securite.passwordChangedAt

  const tabs: { id: TabId; label: string; icon: React.ReactNode }[] = [
    { id: 'profil', label: 'Profil Utilisateur', icon: <User size={18} /> },
    { id: 'preferences', label: 'Préférences', icon: <Palette size={18} /> },
    { id: 'securite', label: 'Sécurité', icon: <Shield size={18} /> },
    { id: 'notifications', label: 'Notifications', icon: <Bell size={18} /> }
  ]

  if (loading) {
    return (
      <div className="settings-page">
        <p className="settings-loading">Chargement...</p>
      </div>
    )
  }

  return (
    <div className="settings-page">
      <header className="settings-header">
        <div>
          <h1>Paramètres du Compte</h1>
          <p className="settings-subtitle">Gérez vos informations personnelles, vos préférences et la sécurité de votre accès.</p>
        </div>
      </header>

      {error && <div className="settings-error">{error}</div>}

      <div className="settings-layout">
        <nav className="settings-tabs">
          {tabs.map((t) => (
            <button
              key={t.id}
              type="button"
              className={`settings-tab ${activeTab === t.id ? 'active' : ''}`}
              onClick={() => setActiveTab(t.id)}
            >
              {t.icon}
              <span>{t.label}</span>
            </button>
          ))}
          <div className="settings-help">
            <p className="settings-help-title">BESOIN D'AIDE ?</p>
            <p className="settings-help-text">Contactez le support IT pour toute modification restreinte.</p>
            <button type="button" className="settings-btn-ticket">Ouvrir un ticket</button>
          </div>
        </nav>

        <main className="settings-content">
          {activeTab === 'profil' && (
            <section className="settings-panel">
              <h2>Profil Utilisateur</h2>
              <p className="settings-desc">Vos informations publiques au sein de CBI Maroc.</p>
              <div className="settings-profile-block">
                <div className="settings-profile-photo-wrap">
                  {profil.photoUrl ? (
                    <img src={profil.photoUrl} alt="" className="settings-profile-photo" />
                  ) : (
                    <div className="settings-profile-avatar">
                      {displayName ? displayName.charAt(0).toUpperCase() : user?.username?.charAt(0).toUpperCase() ?? '?'}
                    </div>
                  )}
                  <button type="button" className="settings-btn-photo">Modifier la photo</button>
                </div>
                <div className="settings-profile-info">
                  <p className="settings-profile-name">
                    {displayName}
                    <CheckCircle size={16} className="settings-check" />
                  </p>
                  <p className="settings-profile-role">{profil.poste || 'Employé'}</p>
                  <div className="settings-profile-badges">
                    <span className="settings-badge">Compte Employé</span>
                    {profil.departement && <span className="settings-badge">{profil.departement}</span>}
                  </div>
                </div>
              </div>
              <div className="settings-form-grid">
                <div className="form-group">
                  <label>Adresse Email Professionnelle</label>
                  <input
                    type="email"
                    value={displayEmail}
                    onChange={(e) => isRhOrAdmin && setProfil((p) => ({ ...p, emailPro: e.target.value }))}
                    readOnly={!isRhOrAdmin}
                    placeholder="email@cbi.ma"
                  />
                </div>
                <div className="form-group">
                  <label>Numéro de Téléphone</label>
                  <input
                    type="tel"
                    value={profil.telephone ?? ''}
                    onChange={(e) => setProfil((p) => ({ ...p, telephone: e.target.value }))}
                    placeholder="+212 6 61 23 45 67"
                  />
                </div>
                <div className="form-group">
                  <label>Département</label>
                  <input
                    type="text"
                    value={profil.departement ?? ''}
                    onChange={(e) => isRhOrAdmin && setProfil((p) => ({ ...p, departement: e.target.value }))}
                    readOnly={!isRhOrAdmin}
                    placeholder="Département IT & Digital"
                  />
                </div>
                <div className="form-group">
                  <label>Localisation</label>
                  <input
                    type="text"
                    value={profil.localisation ?? ''}
                    onChange={(e) => isRhOrAdmin && setProfil((p) => ({ ...p, localisation: e.target.value }))}
                    readOnly={!isRhOrAdmin}
                    placeholder="Siège Social, Casablanca"
                  />
                </div>
              </div>
              <button type="button" className="settings-btn-save" onClick={handleSaveProfil} disabled={saving}>
                <Save size={18} /> Enregistrer les modifications
              </button>
            </section>
          )}

          {activeTab === 'preferences' && (
            <section className="settings-panel">
              <h2>Préférences d'Affichage</h2>
              <p className="settings-desc">Personnalisez votre interface CBI Connect.</p>
              <div className="form-group">
                <label>Langue de l'interface</label>
                <p className="settings-field-desc">Choisissez votre langue préférée.</p>
                <select
                  value={preferences.language}
                  onChange={(e) => setPreferences((p) => ({ ...p, language: e.target.value }))}
                >
                  <option value="fr">Français (France)</option>
                  <option value="en">English</option>
                </select>
              </div>
              <div className="form-group">
                <label>Thème visuel</label>
                <p className="settings-field-desc">Mode clair, sombre ou système.</p>
                <div className="settings-theme-toggle">
                  <button
                    type="button"
                    className={`settings-theme-btn ${preferences.theme === 'light' ? 'active' : ''}`}
                    onClick={() => setPreferences((p) => ({ ...p, theme: 'light' }))}
                  >
                    <Sun size={18} /> Mode Clair
                  </button>
                  <button
                    type="button"
                    className={`settings-theme-btn ${preferences.theme === 'dark' ? 'active' : ''}`}
                    onClick={() => setPreferences((p) => ({ ...p, theme: 'dark' }))}
                  >
                    <Moon size={18} /> Mode Sombre
                  </button>
                </div>
              </div>
              <button type="button" className="settings-btn-save" onClick={handleSavePreferences} disabled={saving}>
                <Save size={18} /> Enregistrer les modifications
              </button>
            </section>
          )}

          {activeTab === 'securite' && (
            <section className="settings-panel">
              <h2>Sécurité & Authentification</h2>
              <p className="settings-desc">Protégez votre compte et vos accès.</p>
              <div className="form-group">
                <label>Authentification à deux facteurs (MFA)</label>
                <p className="settings-field-desc">Ajoutez une couche de sécurité supplémentaire en exigeant un code de vérification sur votre téléphone.</p>
                {mfaError && <p className="settings-inline-error">{mfaError}</p>}
                <div className="settings-toggle-wrap">
                  <button
                    type="button"
                    className={`settings-toggle ${mfaOn ? 'on' : ''}`}
                    onClick={mfaOn ? handleDisableMfa : handleEnableMfa}
                    disabled={mfaLoading}
                  >
                    <span className="settings-toggle-slider" />
                  </button>
                  <span>{mfaOn ? 'Activé' : 'Désactivé'}</span>
                </div>
              </div>
              <div className="form-group">
                <label>Mot de passe de session</label>
                <p className="settings-field-desc">
                  {passwordChangedAt
                    ? `Dernière modification il y a 3 mois. Il est conseillé de le changer tous les 6 mois.`
                    : 'Définir ou modifier votre mot de passe.'}
                </p>
                <button type="button" className="settings-btn-secondary" onClick={() => window.alert('Utilisez la réinitialisation mot de passe ou contactez le support IT.')}>
                  Changer le mot de passe
                </button>
              </div>
              <button type="button" className="settings-btn-save" onClick={handleSaveSecurite} disabled={saving}>
                <Save size={18} /> Enregistrer
              </button>
            </section>
          )}

          {activeTab === 'notifications' && (
            <section className="settings-panel">
              <h2>Gestion des Notifications</h2>
              <p className="settings-desc">Définissez comment et quand vous souhaitez être alerté.</p>
              <div className="settings-notif-item">
                <div>
                  <label>Alertes par Email</label>
                  <p className="settings-field-desc">Recevoir des résumés quotidiens et des notifications urgentes par mail.</p>
                </div>
                <button
                  type="button"
                  className={`settings-toggle ${notifications.emailAlerts ? 'on' : ''}`}
                  onClick={() => setNotifications((n) => ({ ...n, emailAlerts: !n.emailAlerts }))}
                >
                  <span className="settings-toggle-slider" />
                </button>
              </div>
              <div className="settings-notif-item">
                <div>
                  <label>Notifications Push Navigateur</label>
                  <p className="settings-field-desc">Recevoir des alertes en temps réel pour les nouveaux messages et indicateurs.</p>
                </div>
                <button
                  type="button"
                  className={`settings-toggle ${notifications.pushEnabled ? 'on' : ''}`}
                  onClick={() => setNotifications((n) => ({ ...n, pushEnabled: !n.pushEnabled }))}
                >
                  <span className="settings-toggle-slider" />
                </button>
              </div>
              <div className="settings-notif-item settings-notif-disabled">
                <div>
                  <label>Alertes SMS (Bientôt disponible)</label>
                  <p className="settings-field-desc">Recevoir des notifications critiques directement sur votre mobile.</p>
                </div>
                <button type="button" className="settings-toggle" disabled>
                  <span className="settings-toggle-slider" />
                </button>
              </div>
              <p className="settings-notif-note">Note : Les notifications RH critiques ne peuvent pas être désactivées.</p>
              <button type="button" className="settings-btn-save" onClick={handleSaveNotifications} disabled={saving}>
                <Save size={18} /> Enregistrer
              </button>
            </section>
          )}

          {saveSuccess && <span className="settings-save-success">Paramètres enregistrés.</span>}
        </main>
      </div>
    </div>
  )
}
