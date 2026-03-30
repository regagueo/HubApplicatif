import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import {
  Clock,
  CheckCircle,
  AlertCircle,
  FileText,
  Plus,
  ChevronRight
} from 'lucide-react'
import {
  fetchNotesFrais,
  fetchIndicators,
  fetchAlerts
} from '../../../api/employee'
import { getDemandes, type DemandeCongesDto } from '../../../api/conges'
import './employee.css'

interface Notification {
  id: number
  type: 'conges_valide' | 'justificatif_manquant' | 'document_rh'
  titre: string
  message: string
  date: string
}

const defaultNotesFrais = {
  enAttente: 0,
  valide: 0,
  derniersRemboursements: [] as { id: number; libelle: string; montant: number }[]
}

const defaultIndicateurs = { joursPris: 0, joursRestants: 0, totalJours: 22 }

function DonutChart({ joursPris, joursRestants }: { joursPris: number; joursRestants: number }) {
  const total = joursPris + joursRestants
  const pctRestants = total > 0 ? (joursRestants / total) * 100 : 0
  const pctPris = total > 0 ? (joursPris / total) * 100 : 0
  const r = 45
  const circ = 2 * Math.PI * r
  const strokeRestants = (pctRestants / 100) * circ
  const strokePris = (pctPris / 100) * circ

  return (
    <div className="donut-chart">
      <svg viewBox="0 0 100 100" className="donut-svg">
        <circle
          cx="50"
          cy="50"
          r={r}
          fill="none"
          stroke="var(--accent)"
          strokeWidth="12"
          strokeDasharray={`${strokeRestants} ${circ}`}
          strokeDashoffset="0"
          transform="rotate(-90 50 50)"
        />
        <circle
          cx="50"
          cy="50"
          r={r}
          fill="none"
          stroke="var(--warning)"
          strokeWidth="12"
          strokeDasharray={`${strokePris} ${circ}`}
          strokeDashoffset={-strokeRestants}
          transform="rotate(-90 50 50)"
        />
      </svg>
      <div className="donut-center">
        <span className="donut-value">{joursRestants}</span>
        <span className="donut-label">JOURS RESTANTS</span>
      </div>
    </div>
  )
}

export default function DashboardEmployee() {
  const { user } = useAuth()
  const [demandesConges, setDemandesConges] = useState<DemandeCongesDto[]>([])
  const [notesFrais, setNotesFrais] = useState(defaultNotesFrais)
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [indicateurs, setIndicateurs] = useState(defaultIndicateurs)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showAllConges, setShowAllConges] = useState(false)

  const employeeId = (() => {
    const token = localStorage.getItem('portail_auth_token')
    if (token) {
      try {
        const part = token.split('.')[1] || ''
        let base64 = part.replace(/-/g, '+').replace(/_/g, '/')
        const padLen = base64.length % 4
        if (padLen) base64 = base64 + '='.repeat(4 - padLen)
        const payload = JSON.parse(atob(base64))
        const raw = payload?.userId ?? payload?.id
        const parsed = typeof raw === 'number' ? raw : Number(raw)
        if (Number.isFinite(parsed) && parsed > 0) return parsed
      } catch {
        // fallback below
      }
    }
    if (typeof user?.id === 'number' && Number.isFinite(user.id) && user.id > 0) return user.id
    return 0
  })()

  useEffect(() => {
    if (!employeeId) {
      setLoading(false)
      return
    }
    const id = employeeId
    setLoading(true)
    setError(null)
    Promise.all([
      getDemandes(id, new Date().getFullYear())
        .then(setDemandesConges)
        .catch(() => setDemandesConges([])),
      fetchNotesFrais(id).then(setNotesFrais).catch(() => setNotesFrais(defaultNotesFrais)),
      fetchIndicators(id).then((r) => setIndicateurs({
        joursPris: r.joursPris ?? 0,
        joursRestants: r.joursRestants ?? 0,
        totalJours: r.totalJours ?? 22
      })).catch(() => setIndicateurs(defaultIndicateurs)),
      fetchAlerts(id).then((data) => setNotifications(data as Notification[])).catch(() => setNotifications([]))
    ])
      .then(() => setError(null))
      .catch((e) => setError(e instanceof Error ? e.message : 'Erreur chargement'))
      .finally(() => setLoading(false))
  }, [employeeId])

  const getStatutBadge = (statut: string, statutLabel?: string) => {
    const map = {
      SOUMMIS: { label: 'En attente RH/Manager', className: 'statut-attente' },
      EN_ATTENTE: { label: 'En attente RH/Manager', className: 'statut-attente' },
      EN_ATTENTE_MANAGER: { label: 'En attente Manager', className: 'statut-attente' },
      EN_ATTENTE_RH: { label: 'En attente RH', className: 'statut-attente' },
      VALIDE: { label: 'Approuvée', className: 'statut-valide' },
      REJETE: { label: 'Refusée', className: 'statut-refuse' },
      REFUSE: { label: 'Refusée', className: 'statut-refuse' }
    } as Record<string, { label: string; className: string }>
    const { label, className } = map[statut] ?? { label: statutLabel ?? statut, className: 'statut-attente' }
    return <span className={`badge ${className}`}>{label}</span>
  }

  const formatDuree = (dureeJours?: number): string => {
    if (typeof dureeJours !== 'number' || !Number.isFinite(dureeJours) || dureeJours <= 0) return '--'
    if (dureeJours === 0.5) return '0.5 jour'
    if (dureeJours === 1) return '1 jour'
    return `${dureeJours} jours`
  }

  const demandesCongesDisplay = showAllConges ? demandesConges : demandesConges.slice(0, 3)

  const getNotificationIcon = (type: Notification['type']) => {
    switch (type) {
      case 'conges_valide':
        return <Clock size={18} color="var(--success)" />
      case 'justificatif_manquant':
        return <AlertCircle size={18} color="var(--warning)" />
      case 'document_rh':
        return <FileText size={18} color="var(--accent)" />
      default:
        return <CheckCircle size={18} />
    }
  }

  if (loading) {
    return (
      <div className="dashboard-employee">
        <p style={{ color: 'var(--text-secondary)' }}>Chargement du tableau de bord...</p>
      </div>
    )
  }

  if (error) {
    return (
      <div className="dashboard-employee">
        <p style={{ color: 'var(--error)' }}>{error}</p>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
          Vérifiez que employee-service est démarré (port 8082) et que le gateway route /api/employee.
        </p>
      </div>
    )
  }

  return (
    <div className="dashboard-employee">
      <header className="dashboard-employee-header">
        <div>
          <h1>Tableau de bord</h1>
          <p className="dashboard-employee-subtitle">
            Gérez vos demandes et suivez vos indicateurs en temps réel.
          </p>
        </div>
      </header>

      <div className="dashboard-employee-grid">
        {/* Contenu principal */}
        <div className="dashboard-employee-main">
          {/* Mes demandes de congés */}
          <section className="dashboard-card">
            <div className="dashboard-card-header">
              <h2>Mes demandes de congés</h2>
              <Link to="/conges" className="dashboard-card-link">
                <Plus size={16} />
                Nouvelle demande de congé
              </Link>
            </div>
            <div className={`conges-overview-table-wrap ${showAllConges ? 'expanded' : ''}`}>
              <table className="data-table conges-overview-table">
                <thead>
                  <tr>
                    <th>Dates</th>
                    <th>Motif</th>
                    <th>Durée</th>
                    <th>Statut</th>
                    <th>Validateur</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {demandesCongesDisplay.map((d) => {
                    const isPending = d.statut === 'SOUMMIS' || d.statut === 'EN_ATTENTE' || d.statut === 'EN_ATTENTE_MANAGER' || d.statut === 'EN_ATTENTE_RH'
                    return (
                      <tr key={d.id}>
                        <td>{d.dateDebut} - {d.dateFin}</td>
                        <td>{d.motif}</td>
                        <td>{formatDuree(d.dureeJours)}</td>
                        <td>{getStatutBadge(d.statut, d.statutLabel)}</td>
                        <td>{d.validateurNom && d.validateurNom.trim() && d.validateurNom !== '--' ? d.validateurNom : '--'}</td>
                        <td className="conges-overview-actions">
                          <Link to="/conges">Détails</Link>
                          {isPending && <Link to="/conges">Modifier</Link>}
                          {isPending && <Link to="/conges" className="danger">Annuler</Link>}
                        </td>
                      </tr>
                    )
                  })}
                  {demandesCongesDisplay.length === 0 && (
                    <tr>
                      <td colSpan={6}>Aucune demande de congé.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
            {demandesConges.length > 3 && (
              <button type="button" className="dashboard-card-see-more" onClick={() => setShowAllConges((v) => !v)}>
                {showAllConges ? 'Réduire' : 'Voir plus'} <ChevronRight size={16} />
              </button>
            )}
          </section>

          {/* Mes notes de frais */}
          <section className="dashboard-card">
            <div className="dashboard-card-header">
              <h2>Mes notes de frais</h2>
              <Link to="/notes-frais" className="dashboard-card-link">
                Historique <ChevronRight size={16} />
              </Link>
            </div>
            <div className="notes-frais-amounts">
              <div className="amount-block amount-en-attente">
                <span className="amount-label">EN ATTENTE</span>
                <span className="amount-value">{notesFrais.enAttente.toLocaleString('fr-FR', { minimumFractionDigits: 2 })} DH</span>
              </div>
              <div className="amount-block amount-valide">
                <span className="amount-label">VALIDÉ</span>
                <span className="amount-value">{notesFrais.valide.toLocaleString('fr-FR', { minimumFractionDigits: 2 })} DH</span>
              </div>
            </div>
            <div className="remboursements-section">
              <h3>Derniers remboursements</h3>
              <ul className="remboursements-list">
                {notesFrais.derniersRemboursements.map((r) => (
                  <li key={r.id} className="remboursement-item">
                    <span className="remboursement-libelle">{r.libelle}</span>
                    <span className="remboursement-montant">+{r.montant} DH</span>
                  </li>
                ))}
              </ul>
            </div>
            <Link to="/notes-frais" className="btn-primary btn-submit-note">
              <Plus size={18} />
              Soumettre une note de frais
            </Link>
          </section>

          {/* Notifications */}
          <section className="dashboard-card">
            <div className="dashboard-card-header">
              <h2>Notifications</h2>
              <button type="button" className="dashboard-card-link-btn">
                Marquer tout comme lu
              </button>
            </div>
            <ul className="notifications-list">
              {notifications.map((n) => (
                <li key={n.id} className="notification-item">
                  <div className="notification-icon">{getNotificationIcon(n.type)}</div>
                  <div className="notification-content">
                    <span className="notification-titre">{n.titre}</span>
                    <span className="notification-message">{n.message}</span>
                    <span className="notification-date">{n.date}</span>
                  </div>
                </li>
              ))}
            </ul>
          </section>

          {/* Indicateurs personnels */}
          <section className="dashboard-card indicateurs-card">
            <h2>Indicateurs personnels</h2>
            <h3 className="indicateurs-solde-label">SOLDE CONGÉS</h3>
            <div className="indicateurs-donut-wrapper">
              <DonutChart joursPris={indicateurs.joursPris} joursRestants={indicateurs.joursRestants} />
              <div className="indicateurs-legend">
                <span className="legend-item">
                  <span className="legend-dot legend-jours-restants" />
                  Jours restants
                </span>
                <span className="legend-item">
                  <span className="legend-dot legend-jours-pris" />
                  Jours pris
                </span>
              </div>
            </div>
            <p className="indicateurs-jours-text">{indicateurs.joursRestants} JOURS RESTANTS</p>
            <button type="button" className="btn-secondary btn-dossier-rh">
              Voir mon dossier RH complet
            </button>
          </section>
        </div>

      </div>

      {/* Support RH - footer */}
      <footer className="dashboard-support-rh">
        <strong>SUPPORT RH</strong>
        <p>Besoin d&apos;aide ? Contactez l&apos;assistance au 05 XX XXXX</p>
      </footer>
    </div>
  )
}
