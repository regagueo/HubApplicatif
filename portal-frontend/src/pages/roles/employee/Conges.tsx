import { useState, useEffect } from 'react'
import { useAuth } from '../../../context/AuthContext'
import {
  Calendar,
  Clock,
  Info,
  Send,
  ChevronRight
} from 'lucide-react'
import * as api from '../../../api/conges'
import type {
  SoldeCongesDto,
  DemandeCongesDto,
  CreateDemandeRequest,
  HistoriqueSoldeDto,
  CalculJoursOuvresDto
} from '../../../api/conges'
import './Conges.css'

const MOTIFS = [
  { value: 'CONGES_ANNUELS', label: 'Congés Annuels' },
  { value: 'RTT', label: 'RTT' },
  { value: 'EVENEMENT_FAMILIAL', label: 'Événement familial' },
  { value: 'CONGES_EXCEPTIONNELS', label: 'Congés Exceptionnels' },
  { value: 'MALADIE', label: 'Maladie' },
  { value: 'AUTRE', label: 'Autre' }
]

const PERIODES = [
  { value: 'JOURNEE_COMPLETE', label: 'Journée(s) complète(s)' },
  { value: 'DEMI_JOURNEE', label: 'Demi-journée' }
]

const ANNEES = [2026, 2025, 2024]
const STATUTS = [
  { value: 'TOUS', label: 'Tous les statuts' },
  { value: 'EN_ATTENTE_MANAGER', label: 'En attente Manager' },
  { value: 'EN_ATTENTE_RH', label: 'En attente RH' },
  { value: 'VALIDE', label: 'Validé' },
  { value: 'REFUSE', label: 'Refusé' }
]

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/

function normalizeDateInput(value: string): string {
  const trimmed = (value ?? '').trim()
  if (!trimmed) return ''
  if (ISO_DATE_RE.test(trimmed)) return trimmed
  const fr = trimmed.match(/^(\d{2})\/(\d{2})\/(\d{4})$/)
  if (fr) return `${fr[3]}-${fr[2]}-${fr[1]}`
  return ''
}

export default function Conges() {
  const { user } = useAuth()
  const [soldes, setSoldes] = useState<SoldeCongesDto[]>([])
  const [demandes, setDemandes] = useState<DemandeCongesDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [annee, setAnnee] = useState(new Date().getFullYear())
  const [statutFilter, setStatutFilter] = useState('TOUS')
  const [editingId, setEditingId] = useState<number | null>(null)
  const [historiqueSolde, setHistoriqueSolde] = useState<HistoriqueSoldeDto[]>([])
  const [loadingHistSolde, setLoadingHistSolde] = useState(false)
  const [calculPreview, setCalculPreview] = useState<CalculJoursOuvresDto | null>(null)
  const [calculLoading, setCalculLoading] = useState(false)
  const [form, setForm] = useState<CreateDemandeRequest>({
    dateDebut: '',
    dateFin: '',
    motif: 'CONGES_ANNUELS',
    periode: 'JOURNEE_COMPLETE',
    commentaire: ''
  })

  const employeeId = (() => {
    if (typeof user?.id === 'number' && Number.isFinite(user.id) && user.id > 0) return user.id
    const token = localStorage.getItem('portail_auth_token')
    if (!token) return 0
    try {
      const part = token.split('.')[1] || ''
      // JWT = base64url (pas base64). On convertit vers base64 classique pour `atob`.
      let base64 = part.replace(/-/g, '+').replace(/_/g, '/')
      const padLen = base64.length % 4
      if (padLen) base64 = base64 + '='.repeat(4 - padLen)
      const payload = JSON.parse(atob(base64))
      const raw = payload?.userId ?? payload?.id
      const parsed = typeof raw === 'number' ? raw : Number(raw)
      return Number.isFinite(parsed) && parsed > 0 ? parsed : 0
    } catch {
      return 0
    }
  })()
  const roles = user?.roles ?? []
  const isManager = roles.some((r) => r === 'MANAGER' || r === 'ROLE_MANAGER')
  const isRh = roles.some((r) => r === 'RH' || r === 'ROLE_RH' || r === 'ADMIN' || r === 'ROLE_ADMIN')
  const canManageDemandes = isManager || isRh
  const [activeTab, setActiveTab] = useState<'mes-demandes' | 'a-valider'>('mes-demandes')
  const [demandesAValider, setDemandesAValider] = useState<DemandeCongesDto[]>([])
  const [loadingDemandesAValider, setLoadingDemandesAValider] = useState(false)
  const [actionDemandeId, setActionDemandeId] = useState<number | null>(null)
  const [demandesSuivi, setDemandesSuivi] = useState<DemandeCongesDto[]>([])

  useEffect(() => {
    if (!employeeId) {
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    Promise.all([
      api.getSoldes(employeeId).then(setSoldes).catch(() => setSoldes([])),
      api.getDemandes(employeeId, annee, statutFilter === 'TOUS' ? undefined : statutFilter).then(setDemandes).catch(() => setDemandes([]))
    ])
      .catch((e) => setError(e instanceof Error ? e.message : 'Erreur'))
      .finally(() => setLoading(false))
  }, [employeeId, annee, statutFilter])

  useEffect(() => {
    if (!employeeId) return
    setLoadingHistSolde(true)
    api.getHistoriqueSolde(employeeId, annee)
      .then(setHistoriqueSolde)
      .catch(() => setHistoriqueSolde([]))
      .finally(() => setLoadingHistSolde(false))
  }, [employeeId, annee])

  useEffect(() => {
    if (!employeeId) return
    api.getDemandes(employeeId, annee)
      .then(setDemandesSuivi)
      .catch(() => setDemandesSuivi([]))
  }, [employeeId, annee])

  useEffect(() => {
    const dateDebut = normalizeDateInput(form.dateDebut)
    const dateFin = normalizeDateInput(form.dateFin)
    const { periode } = form
    if (!dateDebut || !dateFin || !periode || dateDebut > dateFin) {
      setCalculPreview(null)
      setCalculLoading(false)
      return
    }

    let active = true
    setCalculLoading(true)
    api.calculJoursOuvres(dateDebut, dateFin, periode)
      .then((data) => {
        if (!active) return
        setCalculPreview(data)
      })
      .catch(() => {
        if (!active) return
        setCalculPreview(null)
      })
      .finally(() => {
        if (!active) return
        setCalculLoading(false)
      })

    return () => { active = false }
  }, [form.dateDebut, form.dateFin, form.periode])

  useEffect(() => {
    if (!canManageDemandes || activeTab !== 'a-valider') return
    setLoadingDemandesAValider(true)
    api.getDemandesValidation(isRh ? 'RH' : 'MANAGER')
      .then(setDemandesAValider)
      .catch((e) => setError(e instanceof Error ? e.message : 'Erreur chargement demandes à valider'))
      .finally(() => setLoadingDemandesAValider(false))
  }, [activeTab, canManageDemandes, isRh])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const dateDebut = normalizeDateInput(form.dateDebut)
    const dateFin = normalizeDateInput(form.dateFin)
    if (!employeeId || !dateDebut || !dateFin) {
      setError('Dates invalides. Veuillez sélectionner une période valide.')
      return
    }
    if (dateDebut > dateFin) {
      setError('La date de début doit être antérieure ou égale à la date de fin.')
      return
    }
    try {
      const payload: CreateDemandeRequest = {
        ...form,
        dateDebut,
        dateFin
      }
      if (editingId) {
        await api.updateDemande(employeeId, editingId, payload)
        setEditingId(null)
      } else {
        await api.createDemande(employeeId, payload)
      }
      setForm({ dateDebut: '', dateFin: '', motif: 'CONGES_ANNUELS', periode: 'JOURNEE_COMPLETE', commentaire: '' })
      const [s, d] = await Promise.all([api.getSoldes(employeeId), api.getDemandes(employeeId, annee, statutFilter === 'TOUS' ? undefined : statutFilter)])
      setSoldes(s)
      setDemandes(d)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur')
    }
  }

  const handleAnnulerDemande = async (demandeId: number) => {
    if (!employeeId || !window.confirm('Annuler cette demande ?')) return
    try {
      await api.annulerDemande(employeeId, demandeId)
      setDemandes(await api.getDemandes(employeeId, annee, statutFilter === 'TOUS' ? undefined : statutFilter))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur')
    }
  }

  const refreshDemandesAValider = async () => {
    if (!canManageDemandes) return
    setDemandesAValider(await api.getDemandesValidation(isRh ? 'RH' : 'MANAGER'))
  }

  const handleDecisionManager = async (demandeId: number, action: 'valider' | 'refuser') => {
    try {
      setActionDemandeId(demandeId)
      const validateurNom = user?.firstName && user?.lastName
        ? `${user.firstName} ${user.lastName}`
        : user?.username
      if (action === 'valider') {
        await api.validerDemandeParManager(demandeId, validateurNom)
      } else {
        await api.refuserDemandeParManager(demandeId, validateurNom)
      }
      await refreshDemandesAValider()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur')
    } finally {
      setActionDemandeId(null)
    }
  }

  const handleDecisionRh = async (demandeId: number, action: 'valider' | 'refuser') => {
    try {
      setActionDemandeId(demandeId)
      const validateurNom = user?.firstName && user?.lastName
        ? `${user.firstName} ${user.lastName}`
        : user?.username
      if (action === 'valider') {
        await api.validerDemandeParRh(demandeId, validateurNom)
      } else {
        await api.refuserDemandeParRh(demandeId, validateurNom)
      }
      await refreshDemandesAValider()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Erreur')
    } finally {
      setActionDemandeId(null)
    }
  }

  const soldeByType = (type: string) => soldes.find((s) => s.type === type)
  const dureeLabel = (d: DemandeCongesDto) => (d.dureeJours === 0.5 ? '0.5 jour' : d.dureeJours === 1 ? '1 jour' : `${d.dureeJours} jours`)
  const canEdit = (d: DemandeCongesDto) => d.statut === 'EN_ATTENTE_MANAGER' || d.statut === 'SOUMMIS'
  const pendingDemandes = demandesSuivi
    .filter((d) => d.statut === 'SOUMMIS' || d.statut === 'EN_ATTENTE_MANAGER' || d.statut === 'EN_ATTENTE_RH')
    .sort((a, b) => a.id - b.id)
  const trackedDemande = pendingDemandes[0]
  const trackedSuivi = trackedDemande?.suivi ?? []
  const stepSoumission = trackedSuivi.find((s) => s.etape === 'SOUMISSION')
  const stepManager = trackedSuivi.find((s) => s.etape === 'VALIDATION_MANAGER')
  const stepRh = trackedSuivi.find((s) => s.etape === 'VALIDATION_RH')
  const statusLabel = (status?: string): string => {
    if (status === 'VALIDE') return 'Approuvée'
    if (status === 'REFUSE') return 'Refusée'
    return 'En attente'
  }
  const statusClass = (status?: string): string => {
    if (status === 'VALIDE') return 'conges-step-status-valide'
    if (status === 'REFUSE') return 'conges-step-status-refuse'
    return 'conges-step-status-attente'
  }

  if (loading && !soldes.length) {
    return (
      <div className="conges-page">
        <p className="conges-loading">Chargement...</p>
      </div>
    )
  }

  return (
    <div className="conges-page">
      <header className="conges-header">
        <div>
          <h1>Congés & Absences</h1>
          <p className="conges-subtitle">Gérez vos demandes de temps libre et suivez vos soldes en temps réel.</p>
        </div>
        <button type="button" className="conges-btn-politique">
          Politique de congés
        </button>
      </header>

      {error && (
        <div className="conges-error">
          {error}
        </div>
      )}

      {canManageDemandes && (
        <div className="conges-tabs">
          <button
            type="button"
            className={`conges-tab-btn ${activeTab === 'mes-demandes' ? 'active' : ''}`}
            onClick={() => setActiveTab('mes-demandes')}
          >
            Mes demandes
          </button>
          <button
            type="button"
            className={`conges-tab-btn ${activeTab === 'a-valider' ? 'active' : ''}`}
            onClick={() => setActiveTab('a-valider')}
          >
            Demandes à valider
          </button>
        </div>
      )}

      {activeTab === 'a-valider' ? (
        <section className="conges-historique">
          <h2 className="conges-historique-title">
            <Clock size={20} />
            {isRh ? 'Demandes à traiter (RH)' : 'Demandes à traiter (Manager)'}
          </h2>
          {loadingDemandesAValider ? (
            <p className="conges-empty">Chargement...</p>
          ) : demandesAValider.length === 0 ? (
            <p className="conges-empty">Aucune demande en attente manager.</p>
          ) : (
            <div className="conges-table-wrap">
              <table className="conges-table">
                <thead>
                  <tr>
                    <th>Employé ID</th>
                    <th>Dates</th>
                    <th>Motif</th>
                    <th>Durée</th>
                    <th>Statut</th>
                    <th>Historique</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {demandesAValider.map((d) => (
                    <tr key={d.id}>
                      <td>{d.employeeId}</td>
                      <td>{d.dateDebut === d.dateFin ? d.dateDebut : `${d.dateDebut} - ${d.dateFin}`}</td>
                      <td>{d.motif}</td>
                      <td>{dureeLabel(d)}</td>
                      <td>
                        <span className={`conges-statut conges-statut-${d.statut.toLowerCase()}`}>
                          {d.statutLabel}
                        </span>
                      </td>
                      <td>
                        {(d.suivi ?? [])
                          .filter((s) => s.statut !== 'EN_ATTENTE')
                          .map((s) => `${s.label}: ${s.statut} (${s.date})`)
                          .join(' | ') || '—'}
                      </td>
                      <td>
                        {isManager && d.statut === 'EN_ATTENTE_MANAGER' && (
                          <>
                            <button
                              type="button"
                              className="conges-link"
                              disabled={actionDemandeId === d.id}
                              onClick={() => handleDecisionManager(d.id, 'valider')}
                            >
                              Valider
                            </button>
                            <button
                              type="button"
                              className="conges-link conges-link-danger"
                              disabled={actionDemandeId === d.id}
                              onClick={() => handleDecisionManager(d.id, 'refuser')}
                            >
                              Refuser
                            </button>
                          </>
                        )}
                        {isRh && d.statut === 'EN_ATTENTE_RH' && (
                          <>
                            <button
                              type="button"
                              className="conges-link"
                              disabled={actionDemandeId === d.id}
                              onClick={() => handleDecisionRh(d.id, 'valider')}
                            >
                              Valider RH
                            </button>
                            <button
                              type="button"
                              className="conges-link conges-link-danger"
                              disabled={actionDemandeId === d.id}
                              onClick={() => handleDecisionRh(d.id, 'refuser')}
                            >
                              Refuser RH
                            </button>
                          </>
                        )}
                        {((isManager && d.statut !== 'EN_ATTENTE_MANAGER') || (isRh && d.statut !== 'EN_ATTENTE_RH')) && (
                          <span className="conges-link">Traité</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      ) : (
      <>
      <section className="conges-soldes">
        <div className="conges-solde-card">
          <Calendar size={20} className="conges-solde-icon" />
          <div>
            <span className="conges-solde-label">Congés Annuels</span>
            <span className="conges-solde-value">{soldeByType('ANNUEL')?.joursRestants ?? 0} jours</span>
          </div>
          <Info size={14} className="conges-solde-info" />
        </div>
        <div className="conges-solde-card">
          <Clock size={20} className="conges-solde-icon" />
          <div>
            <span className="conges-solde-label">RTT</span>
            <span className="conges-solde-value">{soldeByType('RTT')?.joursRestants ?? 0} jours</span>
          </div>
          <Info size={14} className="conges-solde-info" />
        </div>
        <div className="conges-solde-card">
          <Clock size={20} className="conges-solde-icon" />
          <div>
            <span className="conges-solde-label">Congés Exceptionnels</span>
            <span className="conges-solde-value">{soldeByType('EXCEPTIONNEL')?.joursRestants ?? 0} jours</span>
          </div>
          <Info size={14} className="conges-solde-info" />
        </div>
        <div className="conges-solde-card">
          <Clock size={20} className="conges-solde-icon" />
          <div>
            <span className="conges-solde-label">Maladie (Total An)</span>
            <span className="conges-solde-value">{soldeByType('MALADIE')?.joursRestants ?? 0} jours</span>
          </div>
          <Info size={14} className="conges-solde-info" />
        </div>
      </section>

      <div className="conges-main-grid">
        <div className="conges-form-section">
          <h2 className="conges-form-title">
            <Send size={20} />
            Nouvelle demande d&apos;absence
          </h2>
          <p className="conges-form-desc">Remplissez les détails ci-dessous pour soumettre votre demande à validation.</p>
          <form onSubmit={handleSubmit} className="conges-form">
            <div className="conges-form-row">
              <div className="conges-form-group">
                <label>Date de début</label>
                <input
                  type="date"
                  value={normalizeDateInput(form.dateDebut)}
                  onChange={(e) => setForm({ ...form, dateDebut: normalizeDateInput(e.target.value) })}
                  required
                />
              </div>
              <div className="conges-form-group">
                <label>Date de fin</label>
                <input
                  type="date"
                  value={normalizeDateInput(form.dateFin)}
                  onChange={(e) => setForm({ ...form, dateFin: normalizeDateInput(e.target.value) })}
                  required
                />
              </div>
            </div>
            <div className="conges-form-group">
              <label>Motif de l&apos;absence</label>
              <select
                value={form.motif}
                onChange={(e) => setForm({ ...form, motif: e.target.value })}
              >
                {MOTIFS.map((m) => (
                  <option key={m.value} value={m.value}>{m.label}</option>
                ))}
              </select>
            </div>
            <div className="conges-form-group">
              <label>Période</label>
              <select
                value={form.periode}
                onChange={(e) => setForm({ ...form, periode: e.target.value })}
              >
                {PERIODES.map((p) => (
                  <option key={p.value} value={p.value}>{p.label}</option>
                ))}
              </select>
            </div>
            <div className="conges-form-group">
              <label>Commentaires (Optionnel)</label>
              <textarea
                value={form.commentaire ?? ''}
                onChange={(e) => setForm({ ...form, commentaire: e.target.value })}
                placeholder="Précisez les détails de votre absence ou l'organisation de votre remplacement..."
                rows={3}
              />
            </div>
            <div className="conges-calcul-preview">
              <h3>Transparence du calcul</h3>
              {calculLoading ? (
                <p className="conges-calcul-preview-loading">Calcul en cours...</p>
              ) : calculPreview ? (
                <div className="conges-calcul-preview-grid">
                  <div className="conges-calcul-preview-item">
                    <span className="conges-calcul-preview-label">Jours ouvrés calculés</span>
                    <strong>{calculPreview.joursOuvres}</strong>
                  </div>
                  <div className="conges-calcul-preview-item">
                    <span className="conges-calcul-preview-label">Week-ends exclus</span>
                    <strong>{calculPreview.weekEndsExclus}</strong>
                  </div>
                  <div className="conges-calcul-preview-item">
                    <span className="conges-calcul-preview-label">Jours fériés exclus</span>
                    <strong>{calculPreview.joursFeriesExclus}</strong>
                  </div>
                </div>
              ) : (
                <p className="conges-calcul-preview-empty">Sélectionnez une période valide pour voir le détail du calcul.</p>
              )}
            </div>
            <div className="conges-form-actions">
              <button type="button" className="conges-btn-secondary" onClick={() => { setEditingId(null); setForm({ dateDebut: '', dateFin: '', motif: 'CONGES_ANNUELS', periode: 'JOURNEE_COMPLETE', commentaire: '' }) }}>
                Annuler
              </button>
              <button type="submit" className="conges-btn-primary">
                Soumettre la demande
                <ChevronRight size={18} />
              </button>
            </div>
          </form>
        </div>

        <aside className="conges-suivi-section">
          <h2 className="conges-suivi-title">Suivi de Validation</h2>
          <p className="conges-suivi-subtitle">DEMANDE EN COURS</p>
          {trackedDemande ? (
            <>
              <p className="conges-form-desc">Demande #{trackedDemande.id} - {trackedDemande.motif}</p>
              <div className="conges-suivi-steps">
                <div className="conges-step conges-step-done">
                  <div className="conges-step-dot" />
                  <span>Soumission</span>
                  <span className={`conges-step-status ${statusClass(stepSoumission?.statut)}`}>{statusLabel(stepSoumission?.statut)}</span>
                  <span className="conges-step-date">{stepSoumission?.date || trackedDemande.dateSoumission}</span>
                </div>
                <div className={`conges-step ${stepManager?.statut === 'EN_ATTENTE' ? 'conges-step-current' : ''}`}>
                  <div className={`conges-step-dot ${stepManager?.statut === 'EN_ATTENTE' ? '' : 'conges-step-dot-dashed'}`} />
                  <span>Validation Manager</span>
                  <span className={`conges-step-status ${statusClass(stepManager?.statut)}`}>{statusLabel(stepManager?.statut)}</span>
                  <span className="conges-step-date">{stepManager?.date || '—'}</span>
                </div>
                <div className={`conges-step ${stepRh?.statut === 'EN_ATTENTE' ? 'conges-step-current' : ''}`}>
                  <div className={`conges-step-dot ${stepRh?.statut === 'EN_ATTENTE' ? '' : 'conges-step-dot-dashed'}`} />
                  <span>Validation RH</span>
                  <span className={`conges-step-status ${statusClass(stepRh?.statut)}`}>{statusLabel(stepRh?.statut)}</span>
                  <span className="conges-step-date">{stepRh?.date || '—'}</span>
                </div>
              </div>
            </>
          ) : (
            <p className="conges-empty">Aucune demande en cours de traitement.</p>
          )}
          <div className="conges-info-box">
            <Info size={18} />
            <p>
              {trackedDemande
                ? 'Le suivi affiche une seule demande active. Une fois validée ou refusée, la suivante apparaît automatiquement.'
                : 'Soumettez une demande pour démarrer le circuit de validation dynamique.'}
            </p>
          </div>
        </aside>
      </div>

      <section className="conges-historique">
        <h2 className="conges-historique-title">
          <Clock size={20} />
          Historique des demandes
        </h2>
        <div className="conges-filters">
          <span>Filtres:</span>
          <select value={annee} onChange={(e) => setAnnee(Number(e.target.value))}>
            {ANNEES.map((y) => (
              <option key={y} value={y}>{y}</option>
            ))}
          </select>
          <select value={statutFilter} onChange={(e) => setStatutFilter(e.target.value)}>
            {STATUTS.map((s) => (
              <option key={s.value} value={s.value}>{s.label}</option>
            ))}
          </select>
        </div>
        <div className="conges-table-wrap">
          <table className="conges-table">
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
              {demandes.map((d) => (
                <tr key={d.id}>
                  <td>{d.dateDebut === d.dateFin ? d.dateDebut : `${d.dateDebut} - ${d.dateFin}`}</td>
                  <td>{d.motif}</td>
                  <td>{dureeLabel(d)}</td>
                  <td>
                    <span className={`conges-statut conges-statut-${d.statut.toLowerCase()}`}>
                      {d.statutLabel}
                    </span>
                  </td>
                  <td>{d.validateurNom}</td>
                  <td>
                    <button type="button" className="conges-link" onClick={() => setError('Affichage des détails indisponible pour le moment.')}>Détails</button>
                    {canEdit(d) && (
                      <>
                        <button type="button" className="conges-link" onClick={() => { setEditingId(d.id); setForm({ dateDebut: normalizeDateInput(d.dateDebut), dateFin: normalizeDateInput(d.dateFin), motif: d.motifCode, periode: d.periode, commentaire: d.commentaire ?? '' }) }}>Modifier</button>
                        <button type="button" className="conges-link conges-link-danger" onClick={() => handleAnnulerDemande(d.id)}>Annuler</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="conges-historique conges-hist-solde">
        <h2 className="conges-historique-title">
          <Clock size={20} />
          Historique des acquisitions &amp; consommations
        </h2>
        {loadingHistSolde ? (
          <p className="conges-empty">Chargement...</p>
        ) : historiqueSolde.length === 0 ? (
          <p className="conges-empty">Aucun mouvement enregistré pour {annee}.</p>
        ) : (
          <div className="conges-table-wrap">
            <table className="conges-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Type</th>
                  <th>Valeur</th>
                  <th>Libellé</th>
                </tr>
              </thead>
              <tbody>
                {historiqueSolde.map((h) => {
                  const isAcq = h.typeMouvement.includes('ACQUISITION')
                  const dateStr = h.dateMouvement
                    ? new Date(h.dateMouvement).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' })
                    : '—'
                  return (
                    <tr key={h.id}>
                      <td>{dateStr}</td>
                      <td>
                        <span className={`conges-mouvement-type ${isAcq ? 'conges-mouvement-acq' : 'conges-mouvement-cons'}`}>
                          {isAcq ? 'Acquisition' : 'Consommation'}
                        </span>
                      </td>
                      <td className={h.valeur >= 0 ? 'conges-val-pos' : 'conges-val-neg'}>
                        {h.valeur > 0 ? '+' : ''}{h.valeur} jour{Math.abs(h.valeur) > 1 ? 's' : ''}
                      </td>
                      <td>{h.libelle}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <footer className="conges-footer">
        <div className="conges-footer-support">
          <strong>SUPPORT RH</strong>
          <p>Besoin d&apos;aide ? Contactez l&apos;assistance au 05 XX XXXX</p>
        </div>
        <p className="conges-footer-copy">© 2026 CBI Maroc - CBI Connect Employee Portal. Tous droits réservés.</p>
      </footer>
      </>
      )}
    </div>
  )
}
