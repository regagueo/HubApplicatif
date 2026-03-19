import { useState, useEffect } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../../../context/AuthContext'
import {
  Clock,
  CheckCircle,
  AlertCircle,
  FileText,
  Plus,
  ChevronRight,
  Users
} from 'lucide-react'
import {
  fetchConges,
  fetchNotesFrais,
  fetchIndicators,
  fetchAlerts,
  fetchColleagues
} from '../../../api/employee'
import './employee.css'

interface DemandeConges {
  id: number
  type: string
  dateDebut: string
  dateFin: string
  statut: 'EN_ATTENTE' | 'VALIDE' | 'REJETE'
}

interface Notification {
  id: number
  type: 'conges_valide' | 'justificatif_manquant' | 'document_rh'
  titre: string
  message: string
  date: string
}

interface Collaborateur {
  id: number
  nom: string
  service: string
  avatar?: string
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
  const [demandesConges, setDemandesConges] = useState<DemandeConges[]>([])
  const [notesFrais, setNotesFrais] = useState(defaultNotesFrais)
  const [notifications, setNotifications] = useState<Notification[]>([])
  const [indicateurs, setIndicateurs] = useState(defaultIndicateurs)
  const [collaborateurs, setCollaborateurs] = useState<Collaborateur[]>([])
  const [searchCollab, setSearchCollab] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user?.id) {
      setLoading(false)
      return
    }
    const id = user.id
    setLoading(true)
    setError(null)
    Promise.all([
      fetchConges(id).then(setDemandesConges).catch(() => setDemandesConges([])),
      fetchNotesFrais(id).then(setNotesFrais).catch(() => setNotesFrais(defaultNotesFrais)),
      fetchIndicators(id).then((r) => setIndicateurs({
        joursPris: r.joursPris ?? 0,
        joursRestants: r.joursRestants ?? 0,
        totalJours: r.totalJours ?? 22
      })).catch(() => setIndicateurs(defaultIndicateurs)),
      fetchAlerts(id).then((data) => setNotifications(data as Notification[])).catch(() => setNotifications([])),
      fetchColleagues(id).then(setCollaborateurs).catch(() => setCollaborateurs([]))
    ])
      .then(() => setError(null))
      .catch((e) => setError(e instanceof Error ? e.message : 'Erreur chargement'))
      .finally(() => setLoading(false))
  }, [user?.id])

  const filteredCollabs = searchCollab.trim()
    ? collaborateurs.filter(
        (c) =>
          c.nom.toLowerCase().includes(searchCollab.toLowerCase()) ||
          c.service.toLowerCase().includes(searchCollab.toLowerCase())
      )
    : collaborateurs

  const getStatutBadge = (statut: DemandeConges['statut']) => {
    const map = {
      EN_ATTENTE: { label: 'En attente', className: 'statut-attente' },
      VALIDE: { label: 'Validé', className: 'statut-valide' },
      REJETE: { label: 'Rejeté', className: 'statut-refuse' }
    }
    const { label, className } = map[statut]
    return <span className={`badge ${className}`}>{label}</span>
  }

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
                Voir tout <ChevronRight size={16} />
              </Link>
            </div>
            <ul className="demandes-list">
              {demandesConges.map((d) => (
                <li key={d.id} className="demande-item">
                  <Clock size={18} className="demande-icon" />
                  <div className="demande-content">
                    <span className="demande-type">{d.type}</span>
                    <span className="demande-dates">
                      {d.dateDebut}
                      {d.dateDebut !== d.dateFin ? ` - ${d.dateFin}` : ''}
                    </span>
                  </div>
                  {getStatutBadge(d.statut)}
                </li>
              ))}
            </ul>
            <Link to="/conges" className="dashboard-card-action">
              <Plus size={16} />
              Nouvelle demande de congé
            </Link>
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

        {/* Sidebar collaborateurs */}
        <aside className="dashboard-collaborateurs">
          <div className="collaborateurs-header">
            <Users size={20} />
            <h2>Collaborateurs</h2>
            <span className="collaborateurs-count">{collaborateurs.length}</span>
          </div>
          <div className="collaborateurs-search">
            <input
              type="search"
              placeholder="Rechercher..."
              value={searchCollab}
              onChange={(e) => setSearchCollab(e.target.value)}
              className="collaborateurs-search-input"
            />
          </div>
          <ul className="collaborateurs-list">
            {filteredCollabs.map((c) => (
              <li key={c.id} className="collaborateur-item">
                <div className="collaborateur-avatar">
                  {c.nom.split(' ').map((n) => n[0]).join('').slice(0, 2)}
                </div>
                <div className="collaborateur-info">
                  <span className="collaborateur-nom">{c.nom}</span>
                  <span className="collaborateur-service">{c.service}</span>
                </div>
              </li>
            ))}
          </ul>
          <button type="button" className="collaborateurs-view-all">
            Voir tous les collègues
          </button>
        </aside>
      </div>

      {/* Support RH - footer */}
      <footer className="dashboard-support-rh">
        <strong>SUPPORT RH</strong>
        <p>Besoin d&apos;aide ? Contactez l&apos;assistance au 05 XX XXXX</p>
      </footer>
    </div>
  )
}
