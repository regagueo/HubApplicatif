import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../../context/AuthContext'
import { Receipt, Plus, Clock, CheckCircle, FileUp, Download, Info } from 'lucide-react'
import * as api from '../../../api/frais'
import type { EncoursFraisDto, DemandeFraisDto, CreateDemandeFraisRequest, PlafondFraisDto, DossierFraisDto } from '../../../api/frais'
import './NotesFrais.css'

const CATEGORIES = [
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'REPAS', label: 'Repas' },
  { value: 'HEBERGEMENT', label: 'Hébergement' },
  { value: 'FOURNITURES', label: 'Fournitures' },
  { value: 'AUTRE', label: 'Autre' }
]

const MODES_TRANSPORT = [
  { value: 'VOITURE_PERSONNELLE', label: 'Voiture personnelle (1,5 DH/km)' },
  { value: 'TRAIN_BUS_AVION', label: 'Train/Bus/Avion (ticket)' },
  { value: 'TAXI_COVOITURAGE', label: 'Taxi/Covoiturage (forfait 50 DH si sans justificatif)' },
  { value: 'PEAGE_CARBURANT', label: 'Péage/Carburant (justificatif obligatoire)' }
]

const STATUT_STYLE: Record<string, string> = {
  EN_ATTENTE_MANAGER: 'statut-attente',
  EN_ATTENTE_COMPTABILITE: 'statut-attente',
  VALIDE: 'statut-valide',
  REFUSE: 'statut-refuse',
  REMBOURSE: 'statut-paye'
}

const SOUMISSION_MODES = [
  { value: 'SIMPLE', label: 'Ajouter une note simple' },
  { value: 'DOSSIER', label: 'Créer un dossier de frais' }
]

export default function NotesFrais() {
  const { user } = useAuth()
  const [encours, setEncours] = useState<EncoursFraisDto | null>(null)
  const [historique, setHistorique] = useState<DemandeFraisDto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm] = useState<CreateDemandeFraisRequest>({
    montant: 0,
    categorie: 'TRANSPORT',
    modeTransport: 'VOITURE_PERSONNELLE',
    kilometres: 0,
    ville: '',
    anneesExperience: 0,
    description: '',
    justificatif: undefined
  })
  const [dragOver, setDragOver] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [showAllHistorique, setShowAllHistorique] = useState(false)
  const roles = user?.roles ?? []
  const isManager = roles.some((r) => r === 'MANAGER' || r === 'ROLE_MANAGER')
  const isRh = roles.some((r) => r === 'RH' || r === 'ROLE_RH' || r === 'ADMIN' || r === 'ROLE_ADMIN')
  const canReview = isManager || isRh
  const canConfigure = isRh
  const [activeTab, setActiveTab] = useState<'mes-notes' | 'a-valider'>('mes-notes')
  const [reviewDossiers, setReviewDossiers] = useState<DossierFraisDto[]>([])
  const [reviewSimpleNotes, setReviewSimpleNotes] = useState<DemandeFraisDto[]>([])
  const [loadingReview, setLoadingReview] = useState(false)
  const [actionId, setActionId] = useState<string | null>(null)
  const [plafonds, setPlafonds] = useState<PlafondFraisDto | null>(null)
  const [savingPlafonds, setSavingPlafonds] = useState(false)
  const [submissionMode, setSubmissionMode] = useState<'SIMPLE' | 'DOSSIER'>('SIMPLE')
  const [dossiers, setDossiers] = useState<DossierFraisDto[]>([])
  const [dossierForm, setDossierForm] = useState({ titre: '', dateDebut: '', dateFin: '' })
  const [dossierNotesDraft, setDossierNotesDraft] = useState<CreateDemandeFraisRequest[]>([])
  const [selectedDossier, setSelectedDossier] = useState<DossierFraisDto | null>(null)
  const [simpleFieldErrors, setSimpleFieldErrors] = useState<Record<string, string>>({})
  const [dossierFieldErrors, setDossierFieldErrors] = useState<Record<string, string>>({})
  const [draftFieldErrors, setDraftFieldErrors] = useState<Record<string, string>>({})

  const employeeId = user?.id ?? 0

  const validateNoteFields = (note: CreateDemandeFraisRequest): Record<string, string> => {
    const errors: Record<string, string> = {}
    if ((note.montant ?? 0) <= 0) errors.montant = 'Montant obligatoire et supérieur à 0'
    if (note.categorie === 'TRANSPORT') {
      if (!note.modeTransport) errors.modeTransport = 'Mode de transport requis'
      if (note.modeTransport === 'VOITURE_PERSONNELLE' && (note.kilometres ?? 0) <= 0) {
        errors.kilometres = 'Kilométrage obligatoire pour voiture personnelle'
      }
      if ((note.modeTransport === 'TRAIN_BUS_AVION' || note.modeTransport === 'PEAGE_CARBURANT') && !note.justificatif) {
        errors.justificatif = 'Justificatif obligatoire'
      }
    }
    if (note.categorie === 'HEBERGEMENT' && !(note.ville && note.ville.trim())) {
      errors.ville = 'Ville requise pour l’hébergement'
    }
    if ((note.categorie === 'FOURNITURES' || note.categorie === 'AUTRE' || note.categorie === 'HEBERGEMENT') && !note.justificatif) {
      errors.justificatif = 'Justificatif obligatoire'
    }
    return errors
  }

  const load = useCallback(async () => {
    if (!employeeId) return
    setLoading(true)
    setError(null)
    const [encRes, histRes, dosRes] = await Promise.allSettled([
      api.getEncours(employeeId),
      api.getHistorique(employeeId),
      api.getDossiers(employeeId)
    ])

    const errors: string[] = []

    if (encRes.status === 'fulfilled') {
      setEncours(encRes.value)
    } else {
      setEncours({ montantEncours: 0, demandesEncours: [] })
      errors.push(encRes.reason instanceof Error ? encRes.reason.message : 'Erreur chargement encours')
    }

    if (histRes.status === 'fulfilled') {
      setHistorique(histRes.value)
    } else {
      setHistorique([])
      errors.push(histRes.reason instanceof Error ? histRes.reason.message : 'Erreur chargement historique')
    }

    if (dosRes.status === 'fulfilled') {
      setDossiers(dosRes.value)
    } else {
      setDossiers([])
      errors.push(dosRes.reason instanceof Error ? dosRes.reason.message : 'Erreur chargement dossiers')
    }

    setError(errors.length > 0 ? errors.join(' | ') : null)
    setLoading(false)
  }, [employeeId])

  useEffect(() => {
    load()
  }, [load])

  useEffect(() => {
    if (!canReview || activeTab !== 'a-valider') return
    setLoadingReview(true)
    Promise.all([
      api.getDossiersValidation(isRh ? 'RH' : 'MANAGER'),
      api.getDemandesValidation(isRh ? 'RH' : 'MANAGER')
    ])
      .then(([dossiersRes, demandesRes]) => {
        setReviewDossiers(dossiersRes)
        setReviewSimpleNotes(demandesRes.filter((d) => !d.dossierId))
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Erreur'))
      .finally(() => setLoadingReview(false))
  }, [activeTab, canReview, isRh])

  useEffect(() => {
    if (!canConfigure) return
    api.getPlafondsFrais().then(setPlafonds).catch(() => undefined)
  }, [canConfigure])

  useEffect(() => {
    if (form.categorie !== 'TRANSPORT' || form.modeTransport !== 'VOITURE_PERSONNELLE') return
    const tarifKm = plafonds?.transportTarifKm ?? 1.5
    const km = form.kilometres ?? 0
    const montantAuto = Number((km * tarifKm).toFixed(2))
    setForm((prev) => ({ ...prev, montant: montantAuto }))
  }, [form.categorie, form.modeTransport, form.kilometres, plafonds?.transportTarifKm])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const simpleErrors = validateNoteFields(form)
    setSimpleFieldErrors(simpleErrors)
    if (Object.keys(simpleErrors).length > 0) {
      setError('Erreur de création de la note. Corrigez les champs en rouge.')
      return
    }
    const isVoiturePerso = form.categorie === 'TRANSPORT' && form.modeTransport === 'VOITURE_PERSONNELLE'
    const montantFinal = isVoiturePerso
      ? Number((((form.kilometres ?? 0) * (plafonds?.transportTarifKm ?? 1.5)).toFixed(2)))
      : form.montant
    if (!employeeId || montantFinal <= 0) return
    setSubmitting(true)
    setError(null)
    try {
      await api.createDemande(employeeId, { ...form, montant: montantFinal })
      setForm({
        montant: 0,
        categorie: 'TRANSPORT',
        modeTransport: 'VOITURE_PERSONNELLE',
        kilometres: 0,
        ville: '',
        anneesExperience: 0,
        description: '',
        justificatif: undefined
      })
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur soumission')
    } finally {
      setSubmitting(false)
    }
  }

  const handleExportPdf = async () => {
    if (!employeeId) return
    setExporting(true)
    try {
      const blob = await api.exportPdf(employeeId)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = 'historique-frais.pdf'
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur export')
    } finally {
      setExporting(false)
    }
  }

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragOver(false)
    const f = e.dataTransfer.files[0]
    if (f && /\.(pdf|jpg|jpeg|png)$/i.test(f.name) && f.size <= 5 * 1024 * 1024) {
      setForm((prev) => ({ ...prev, justificatif: f }))
    }
  }
  const onDragOver = (e: React.DragEvent) => { e.preventDefault(); setDragOver(true) }
  const onDragLeave = () => setDragOver(false)

  const refreshReviewDossiers = async () => {
    if (!canReview) return
    const [dossiersRes, demandesRes] = await Promise.all([
      api.getDossiersValidation(isRh ? 'RH' : 'MANAGER'),
      api.getDemandesValidation(isRh ? 'RH' : 'MANAGER')
    ])
    setReviewDossiers(dossiersRes)
    setReviewSimpleNotes(demandesRes.filter((d) => !d.dossierId))
  }

  const getReviewerName = () => (
    user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : user?.username
  )

  const handleManagerDecision = async (dossierId: number, decision: 'valider' | 'refuser') => {
    try {
      setActionId(`dossier-${dossierId}`)
      if (decision === 'valider') {
        await api.validerDossierParManager(dossierId, getReviewerName())
      } else {
        await api.refuserDossierParManager(dossierId, getReviewerName())
      }
      await refreshReviewDossiers()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur')
    } finally {
      setActionId(null)
    }
  }

  const handleManagerDecisionSimple = async (demandeId: number, decision: 'valider' | 'refuser') => {
    try {
      setActionId(`note-${demandeId}`)
      if (decision === 'valider') {
        await api.validerDemandeParManager(demandeId, getReviewerName())
      } else {
        await api.refuserDemandeParManager(demandeId, getReviewerName())
      }
      await refreshReviewDossiers()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur')
    } finally {
      setActionId(null)
    }
  }

  const handleRhDecision = async (dossierId: number, decision: 'valider' | 'refuser') => {
    try {
      setActionId(`dossier-${dossierId}`)
      if (decision === 'valider') {
        await api.validerDossierParRh(dossierId, getReviewerName())
      } else {
        await api.refuserDossierParRh(dossierId, getReviewerName())
      }
      await refreshReviewDossiers()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur')
    } finally {
      setActionId(null)
    }
  }

  const handleRhDecisionSimple = async (demandeId: number, decision: 'valider' | 'refuser') => {
    try {
      setActionId(`note-${demandeId}`)
      if (decision === 'valider') {
        await api.validerDemandeParRh(demandeId, getReviewerName())
      } else {
        await api.refuserDemandeParRh(demandeId, getReviewerName())
      }
      await refreshReviewDossiers()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur')
    } finally {
      setActionId(null)
    }
  }

  const handleSavePlafonds = async () => {
    if (!plafonds) return
    try {
      setSavingPlafonds(true)
      const modifiePar = user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : user?.username
      const updated = await api.updatePlafondsFrais(plafonds, modifiePar)
      setPlafonds(updated)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur mise à jour plafonds')
    } finally {
      setSavingPlafonds(false)
    }
  }

  const addDraftNote = () => {
    const draftErrors = validateNoteFields(form)
    setDraftFieldErrors(draftErrors)
    if (Object.keys(draftErrors).length > 0) {
      setError('Erreur de création du dossier. Corrigez les champs de la note.')
      return
    }
    setError(null)
    setDraftFieldErrors({})
    setDossierNotesDraft((prev) => [...prev, { ...form }])
    setForm({
      montant: 0,
      categorie: 'TRANSPORT',
      modeTransport: 'VOITURE_PERSONNELLE',
      kilometres: 0,
      ville: '',
      anneesExperience: 0,
      description: '',
      justificatif: undefined
    })
  }

  const handleSubmitDossier = async () => {
    if (!employeeId) return
    const dossierErrors: Record<string, string> = {}
    if (!dossierForm.titre.trim()) dossierErrors.titre = 'Titre du dossier manquant'
    if (!dossierForm.dateDebut) dossierErrors.dateDebut = 'Date de début requise'
    if (!dossierForm.dateFin) dossierErrors.dateFin = 'Date de fin requise'
    if (dossierForm.dateDebut && dossierForm.dateFin && dossierForm.dateDebut > dossierForm.dateFin) {
      dossierErrors.dateFin = 'La date de fin doit être après la date de début'
    }
    if (dossierNotesDraft.length === 0) dossierErrors.notesCount = 'Ajoutez au moins une note au dossier'
    setDossierFieldErrors(dossierErrors)
    if (Object.keys(dossierErrors).length > 0) {
      setError('Erreur de création du dossier. Corrigez les champs indiqués.')
      return
    }
    setSubmitting(true)
    setError(null)
    try {
      const dossier = await api.createDossier(employeeId, dossierForm.titre, dossierForm.dateDebut, dossierForm.dateFin)
      for (const note of dossierNotesDraft) {
        await api.addNoteToDossier(employeeId, dossier.id, note)
      }
      await api.submitDossier(employeeId, dossier.id)
      setDossierForm({ titre: '', dateDebut: '', dateFin: '' })
      setDossierNotesDraft([])
      setSubmissionMode('SIMPLE')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur soumission dossier')
    } finally {
      setSubmitting(false)
    }
  }

  const openDossierDetails = async (id: number) => {
    try {
      const full = await api.getDossier(id)
      setSelectedDossier(full)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur chargement dossier')
    }
  }

  const demandesEncours = encours?.demandesEncours ?? []
  const firstEncours = demandesEncours[0]
  const parseReviewDate = (value: string): number => {
    if (!value) return 0
    const direct = new Date(value).getTime()
    if (!Number.isNaN(direct)) return direct
    const fr = value.match(/^(\d{2})\/(\d{2})\/(\d{4})(?:[ T](\d{2}):(\d{2})(?::(\d{2}))?)?$/)
    if (!fr) return 0
    const [, dd, mm, yyyy, hh = '00', min = '00', ss = '00'] = fr
    return new Date(
      Number(yyyy),
      Number(mm) - 1,
      Number(dd),
      Number(hh),
      Number(min),
      Number(ss)
    ).getTime()
  }
  const parseAnyDate = (value?: string): number => parseReviewDate(value ?? '')
  const combinedHistorique = [
    ...historique.map((n) => ({
      kind: 'NOTE' as const,
      id: n.id,
      date: n.dateSoumission,
      sortDate: n.dateSoumissionIso ?? n.dateSoumission,
      note: n
    })),
    ...dossiers.map((d) => ({
      kind: 'DOSSIER' as const,
      id: d.id,
      date: d.dateSoumission || d.dateCreation,
      sortDate: d.dateSoumissionIso ?? d.dateCreationIso ?? d.dateSoumission ?? d.dateCreation,
      dossier: d
    }))
  ].sort((a, b) => {
    const da = parseAnyDate(a.sortDate)
    const db = parseAnyDate(b.sortDate)
    if (db !== da) return db - da
    return b.id - a.id
  })
  const combinedHistoriqueDisplay = showAllHistorique ? combinedHistorique : combinedHistorique.slice(0, 10)
  const reviewItems = [
    ...reviewDossiers.map((d) => ({
      kind: 'DOSSIER' as const,
      id: d.id,
      date: d.dateSoumission || d.dateCreation || '',
      sortDate: d.dateSoumissionIso ?? d.dateCreationIso ?? d.dateSoumission ?? d.dateCreation ?? '',
      dossier: d
    })),
    ...reviewSimpleNotes.map((n) => ({
      kind: 'NOTE' as const,
      id: n.id,
      date: n.dateSoumission || '',
      sortDate: n.dateSoumissionIso ?? n.dateSoumission ?? '',
      note: n
    }))
  ].sort((a, b) => {
    const da = parseReviewDate(a.sortDate)
    const db = parseReviewDate(b.sortDate)
    if (db !== da) return db - da
    if (a.kind !== b.kind) return a.kind === 'NOTE' ? -1 : 1
    return b.id - a.id
  })

  if (loading && !encours) {
    return (
      <div className="notes-frais-page">
        <p className="notes-frais-loading">Chargement...</p>
      </div>
    )
  }

  return (
    <div className="notes-frais-page">
      <header className="notes-frais-header">
        <div>
          <h1><Receipt size={28} /> Notes de frais</h1>
          <p className="notes-frais-subtitle">Gérez vos remboursements et suivez l'état de vos demandes.</p>
        </div>
        <div className="notes-frais-encours-card">
          <span className="notes-frais-encours-label">ENCOURS</span>
          <span className="notes-frais-encours-montant">
            {(encours?.montantEncours ?? 0).toLocaleString('fr-FR', { minimumFractionDigits: 2 })} MAD
          </span>
        </div>
      </header>

      {error && (
        <div className="notes-frais-error">
          {error}
        </div>
      )}

      {canReview && (
        <div className="notes-frais-tabs">
          <button
            type="button"
            className={`notes-frais-tab-btn ${activeTab === 'mes-notes' ? 'active' : ''}`}
            onClick={() => setActiveTab('mes-notes')}
          >
            Mes notes
          </button>
          <button
            type="button"
            className={`notes-frais-tab-btn ${activeTab === 'a-valider' ? 'active' : ''}`}
            onClick={() => setActiveTab('a-valider')}
          >
            Demandes à valider
          </button>
        </div>
      )}

      {activeTab === 'a-valider' ? (
        <section className="notes-frais-card">
          <h2><Clock size={20} /> {isRh ? 'Dossiers et notes simples à traiter (RH)' : 'Dossiers et notes simples à traiter (Manager)'}</h2>
          {loadingReview ? (
            <p className="notes-frais-loading">Chargement...</p>
          ) : reviewItems.length === 0 ? (
            <p className="notes-frais-form-desc">Aucune demande à afficher.</p>
          ) : (
            <div className="notes-frais-table-wrap">
              <table className="notes-frais-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Type</th>
                    <th>Employé ID</th>
                    <th>Référence</th>
                    <th>Montant</th>
                    <th>Statut</th>
                    <th>Historique</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {reviewItems.map((item) => {
                    if (item.kind === 'DOSSIER') {
                      const d = item.dossier
                      return (
                        <tr key={`dossier-${d.id}`}>
                          <td>{d.dateSoumission || d.dateCreation}</td>
                          <td><span className="notes-frais-badge-cat">Dossier</span></td>
                          <td>{d.employeeId}</td>
                          <td>
                            <button type="button" className="notes-frais-link-tout" onClick={() => openDossierDetails(d.id)}>
                              {d.titre}
                            </button>
                          </td>
                          <td>{Number(d.montantTotal).toLocaleString('fr-FR', { minimumFractionDigits: 2 })} MAD</td>
                          <td>
                            <span className={`notes-frais-badge-statut ${STATUT_STYLE[d.statut] ?? ''}`}>{d.statutLabel}</span>
                          </td>
                          <td>
                            {[
                              d.managerNom ? `Manager: ${d.managerNom}` : null,
                              d.rhNom ? `RH: ${d.rhNom}` : null
                            ].filter(Boolean).join(' | ') || '—'}
                          </td>
                          <td>
                            {isManager && d.statut === 'EN_ATTENTE_MANAGER' && (
                              <>
                                <button type="button" className="notes-frais-action-btn" disabled={actionId === `dossier-${d.id}`} onClick={() => handleManagerDecision(d.id, 'valider')}>Valider</button>
                                <button type="button" className="notes-frais-action-btn danger" disabled={actionId === `dossier-${d.id}`} onClick={() => handleManagerDecision(d.id, 'refuser')}>Refuser</button>
                              </>
                            )}
                            {isRh && d.statut === 'EN_ATTENTE_RH' && (
                              <>
                                <button type="button" className="notes-frais-action-btn" disabled={actionId === `dossier-${d.id}`} onClick={() => handleRhDecision(d.id, 'valider')}>Valider RH</button>
                                <button type="button" className="notes-frais-action-btn danger" disabled={actionId === `dossier-${d.id}`} onClick={() => handleRhDecision(d.id, 'refuser')}>Refuser RH</button>
                              </>
                            )}
                            {((isManager && d.statut !== 'EN_ATTENTE_MANAGER') || (isRh && d.statut !== 'EN_ATTENTE_RH')) && (
                              <span className="notes-frais-action-done">Traité</span>
                            )}
                          </td>
                        </tr>
                      )
                    }
                    const n = item.note
                    return (
                      <tr key={`note-${n.id}`}>
                        <td>{n.dateSoumission}</td>
                        <td><span className="notes-frais-badge-cat">Note simple</span></td>
                        <td>{n.employeeId}</td>
                        <td>{n.description || n.reference}</td>
                        <td>{Number(n.montant).toLocaleString('fr-FR', { minimumFractionDigits: 2 })} MAD</td>
                        <td>
                          <span className={`notes-frais-badge-statut ${STATUT_STYLE[n.statut] ?? ''}`}>{n.statutLabel}</span>
                        </td>
                        <td>
                          {[n.managerNom ? `Manager: ${n.managerNom}` : null, n.rhNom ? `RH: ${n.rhNom}` : null].filter(Boolean).join(' | ') || '—'}
                        </td>
                        <td>
                          {isManager && n.statut === 'EN_ATTENTE_MANAGER' && (
                            <>
                              <button type="button" className="notes-frais-action-btn" disabled={actionId === `note-${n.id}`} onClick={() => handleManagerDecisionSimple(n.id, 'valider')}>Valider</button>
                              <button type="button" className="notes-frais-action-btn danger" disabled={actionId === `note-${n.id}`} onClick={() => handleManagerDecisionSimple(n.id, 'refuser')}>Refuser</button>
                            </>
                          )}
                          {isRh && (n.statut === 'EN_ATTENTE_RH' || n.statut === 'EN_ATTENTE_COMPTABILITE') && (
                            <>
                              <button type="button" className="notes-frais-action-btn" disabled={actionId === `note-${n.id}`} onClick={() => handleRhDecisionSimple(n.id, 'valider')}>Valider RH</button>
                              <button type="button" className="notes-frais-action-btn danger" disabled={actionId === `note-${n.id}`} onClick={() => handleRhDecisionSimple(n.id, 'refuser')}>Refuser RH</button>
                            </>
                          )}
                          {((isManager && n.statut !== 'EN_ATTENTE_MANAGER') || (isRh && n.statut !== 'EN_ATTENTE_RH' && n.statut !== 'EN_ATTENTE_COMPTABILITE')) && (
                            <span className="notes-frais-action-done">Traité</span>
                          )}
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>
      ) : (
      <>
      {canConfigure && plafonds && (
        <section className="notes-frais-card">
          <h2>Paramétrage RH des plafonds</h2>
          <div className="notes-frais-form-row">
            <div className="notes-frais-form-group">
              <label>Tarif km voiture (DH)</label>
              <input type="number" step="0.01" value={plafonds.transportTarifKm} onChange={(e) => setPlafonds({ ...plafonds, transportTarifKm: parseFloat(e.target.value) || 0 })} />
            </div>
            <div className="notes-frais-form-group">
              <label>Forfait taxi sans justificatif (DH)</label>
              <input type="number" step="0.01" value={plafonds.taxiForfaitSansJustificatif} onChange={(e) => setPlafonds({ ...plafonds, taxiForfaitSansJustificatif: parseFloat(e.target.value) || 0 })} />
            </div>
            <div className="notes-frais-form-group">
              <label>Repas &lt; 3 ans (DH)</label>
              <input type="number" step="0.01" value={plafonds.repasMoins3Ans} onChange={(e) => setPlafonds({ ...plafonds, repasMoins3Ans: parseFloat(e.target.value) || 0 })} />
            </div>
            <div className="notes-frais-form-group">
              <label>Repas 3+ ans (DH)</label>
              <input type="number" step="0.01" value={plafonds.repas3AnsEtPlus} onChange={(e) => setPlafonds({ ...plafonds, repas3AnsEtPlus: parseFloat(e.target.value) || 0 })} />
            </div>
            <div className="notes-frais-form-group">
              <label>Repas 3+ max (DH)</label>
              <input type="number" step="0.01" value={plafonds.repas3AnsEtPlusMax} onChange={(e) => setPlafonds({ ...plafonds, repas3AnsEtPlusMax: parseFloat(e.target.value) || 0 })} />
            </div>
            <div className="notes-frais-form-group">
              <label>Hébergement Casablanca (DH)</label>
              <input type="number" step="0.01" value={plafonds.hebergementCasablanca} onChange={(e) => setPlafonds({ ...plafonds, hebergementCasablanca: parseFloat(e.target.value) || 0 })} />
            </div>
            <div className="notes-frais-form-group">
              <label>Hébergement autres villes (DH)</label>
              <input type="number" step="0.01" value={plafonds.hebergementAutresVilles} onChange={(e) => setPlafonds({ ...plafonds, hebergementAutresVilles: parseFloat(e.target.value) || 0 })} />
            </div>
            <div className="notes-frais-form-group">
              <label>Per diem global (DH)</label>
              <input type="number" step="0.01" value={plafonds.perDiemGlobal} onChange={(e) => setPlafonds({ ...plafonds, perDiemGlobal: parseFloat(e.target.value) || 0 })} />
            </div>
            <div className="notes-frais-form-group">
              <label>Per diem actif</label>
              <select value={plafonds.perDiemActif ? 'oui' : 'non'} onChange={(e) => setPlafonds({ ...plafonds, perDiemActif: e.target.value === 'oui' })}>
                <option value="non">Non</option>
                <option value="oui">Oui</option>
              </select>
            </div>
          </div>
          <button type="button" className="notes-frais-btn-submit" onClick={handleSavePlafonds} disabled={savingPlafonds}>
            {savingPlafonds ? 'Enregistrement...' : 'Enregistrer plafonds'}
          </button>
        </section>
      )}

      {/* Nouvelle demande */}
      <section className="notes-frais-card notes-frais-form-card">
        <h2><Plus size={20} /> Nouvelle Demande</h2>
        <p className="notes-frais-form-desc">Choisissez un mode de soumission (note simple ou dossier de frais).</p>
        <div className="notes-frais-form-group">
          <label>Mode de soumission</label>
          <select value={submissionMode} onChange={(e) => setSubmissionMode(e.target.value as 'SIMPLE' | 'DOSSIER')}>
            {SOUMISSION_MODES.map((m) => (
              <option key={m.value} value={m.value}>{m.label}</option>
            ))}
          </select>
        </div>
        {submissionMode === 'SIMPLE' ? (
        <form onSubmit={handleSubmit} className="notes-frais-form">
          <div className="notes-frais-form-row">
            <div className="notes-frais-form-group">
              <label>Montant (MAD)</label>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.montant || ''}
                onChange={(e) => { setForm({ ...form, montant: parseFloat(e.target.value) || 0 }); setSimpleFieldErrors((p) => ({ ...p, montant: '' })) }}
                placeholder="MAD 0.00"
                required
                disabled={form.categorie === 'TRANSPORT' && form.modeTransport === 'VOITURE_PERSONNELLE'}
                className={simpleFieldErrors.montant ? 'notes-frais-input-error' : ''}
              />
              {simpleFieldErrors.montant && <span className="notes-frais-field-error">{simpleFieldErrors.montant}</span>}
              {form.categorie === 'TRANSPORT' && form.modeTransport === 'VOITURE_PERSONNELLE' && (
                <span className="notes-frais-upload-hint">
                  Montant calculé automatiquement: {(plafonds?.transportTarifKm ?? 1.5).toFixed(2)} DH/km
                </span>
              )}
            </div>
            <div className="notes-frais-form-group">
              <label>Catégorie</label>
              <select
                value={form.categorie}
                onChange={(e) => { setForm({ ...form, categorie: e.target.value }); setSimpleFieldErrors((p) => ({ ...p, categorie: '', ville: '', justificatif: '' })) }}
              >
                {CATEGORIES.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>
          </div>
          {form.categorie === 'TRANSPORT' && (
            <>
              <div className="notes-frais-form-group">
                <label>Mode de transport</label>
                <select
                  value={form.modeTransport ?? 'VOITURE_PERSONNELLE'}
                  onChange={(e) => { setForm({ ...form, modeTransport: e.target.value }); setSimpleFieldErrors((p) => ({ ...p, modeTransport: '', justificatif: '' })) }}
                  className={simpleFieldErrors.modeTransport ? 'notes-frais-input-error' : ''}
                >
                  {MODES_TRANSPORT.map((m) => (
                    <option key={m.value} value={m.value}>{m.label}</option>
                  ))}
                </select>
                {simpleFieldErrors.modeTransport && <span className="notes-frais-field-error">{simpleFieldErrors.modeTransport}</span>}
              </div>
              {form.modeTransport === 'VOITURE_PERSONNELLE' && (
                <div className="notes-frais-form-group">
                  <label>Kilométrage</label>
                  <input
                    type="number"
                    min="0"
                    step="0.1"
                    value={form.kilometres || ''}
                    onChange={(e) => { setForm({ ...form, kilometres: parseFloat(e.target.value) || 0 }); setSimpleFieldErrors((p) => ({ ...p, kilometres: '' })) }}
                    required
                    className={simpleFieldErrors.kilometres ? 'notes-frais-input-error' : ''}
                  />
                  {simpleFieldErrors.kilometres && <span className="notes-frais-field-error">{simpleFieldErrors.kilometres}</span>}
                </div>
              )}
            </>
          )}
          {form.categorie === 'HEBERGEMENT' && (
            <div className="notes-frais-form-group">
              <label>Ville</label>
              <input
                type="text"
                value={form.ville ?? ''}
                onChange={(e) => { setForm({ ...form, ville: e.target.value }); setSimpleFieldErrors((p) => ({ ...p, ville: '' })) }}
                placeholder="Ex: Casablanca"
                required
                className={simpleFieldErrors.ville ? 'notes-frais-input-error' : ''}
              />
              {simpleFieldErrors.ville && <span className="notes-frais-field-error">{simpleFieldErrors.ville}</span>}
            </div>
          )}
          {form.categorie === 'REPAS' && (
            <div className="notes-frais-form-group">
              <label>Années d&apos;expérience</label>
              <input
                type="number"
                min="0"
                step="1"
                value={form.anneesExperience ?? 0}
                onChange={(e) => setForm({ ...form, anneesExperience: parseInt(e.target.value || '0', 10) })}
              />
            </div>
          )}
          <div className="notes-frais-form-group">
            <label>Description / Motif</label>
            <textarea
              value={form.description ?? ''}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              placeholder="Ex: Déjeuner client projet Phoenix..."
              rows={3}
            />
          </div>
          <div className="notes-frais-form-group">
            <label>Justificatif (PDF, JPG, PNG)</label>
            <div
              className={`notes-frais-upload ${dragOver ? 'drag-over' : ''}`}
              onDrop={onDrop}
              onDragOver={onDragOver}
              onDragLeave={onDragLeave}
            >
              <FileUp size={32} />
              <p>Cliquez ou glissez-déposez</p>
              <span className="notes-frais-upload-hint">Taille maximale 5MB. Un reçu par fichier.</span>
              <input
                type="file"
                accept=".pdf,.jpg,.jpeg,.png"
                onChange={(e) => { setForm({ ...form, justificatif: e.target.files?.[0] }); setSimpleFieldErrors((p) => ({ ...p, justificatif: '' })) }}
                className="notes-frais-upload-input"
              />
              {form.justificatif && (
                <p className="notes-frais-upload-file">{form.justificatif.name}</p>
              )}
            </div>
            {simpleFieldErrors.justificatif && <span className="notes-frais-field-error">{simpleFieldErrors.justificatif}</span>}
          </div>
          <button type="submit" className="notes-frais-btn-submit" disabled={submitting}>
            <Receipt size={18} /> Soumettre la Note de Frais
          </button>
        </form>
        ) : (
          <div className="notes-frais-form">
            <h3>Créer un dossier de frais</h3>
            <div className="notes-frais-form-group">
              <label>Titre du dossier</label>
              <input
                value={dossierForm.titre}
                onChange={(e) => { setDossierForm({ ...dossierForm, titre: e.target.value }); setDossierFieldErrors((p) => ({ ...p, titre: '' })) }}
                placeholder="Mission Casablanca - Mars"
                className={dossierFieldErrors.titre ? 'notes-frais-input-error' : ''}
              />
              {dossierFieldErrors.titre && <span className="notes-frais-field-error">{dossierFieldErrors.titre}</span>}
            </div>
            <div className="notes-frais-form-row">
              <div className="notes-frais-form-group">
                <label>Date début</label>
                <input
                  type="date"
                  value={dossierForm.dateDebut}
                  onChange={(e) => { setDossierForm({ ...dossierForm, dateDebut: e.target.value }); setDossierFieldErrors((p) => ({ ...p, dateDebut: '' })) }}
                  className={dossierFieldErrors.dateDebut ? 'notes-frais-input-error' : ''}
                />
                {dossierFieldErrors.dateDebut && <span className="notes-frais-field-error">{dossierFieldErrors.dateDebut}</span>}
              </div>
              <div className="notes-frais-form-group">
                <label>Date fin</label>
                <input
                  type="date"
                  value={dossierForm.dateFin}
                  onChange={(e) => { setDossierForm({ ...dossierForm, dateFin: e.target.value }); setDossierFieldErrors((p) => ({ ...p, dateFin: '' })) }}
                  className={dossierFieldErrors.dateFin ? 'notes-frais-input-error' : ''}
                />
                {dossierFieldErrors.dateFin && <span className="notes-frais-field-error">{dossierFieldErrors.dateFin}</span>}
              </div>
            </div>
            <h3>Ajouter une note au dossier</h3>
            <div className="notes-frais-form-row">
              <div className="notes-frais-form-group">
                <label>Montant (MAD)</label>
                <input type="number" min="0" step="0.01" value={form.montant || ''} onChange={(e) => setForm({ ...form, montant: parseFloat(e.target.value) || 0 })} />
                {draftFieldErrors.montant && <span className="notes-frais-field-error">{draftFieldErrors.montant}</span>}
              </div>
              <div className="notes-frais-form-group">
                <label>Catégorie</label>
                <select value={form.categorie} onChange={(e) => { setForm({ ...form, categorie: e.target.value }); setDraftFieldErrors((p) => ({ ...p, ville: '', justificatif: '' })) }}>
                  {CATEGORIES.map((c) => (
                    <option key={c.value} value={c.value}>{c.label}</option>
                  ))}
                </select>
              </div>
            </div>
            {form.categorie === 'TRANSPORT' && (
              <>
                <div className="notes-frais-form-group">
                  <label>Mode de transport</label>
                  <select value={form.modeTransport ?? 'VOITURE_PERSONNELLE'} onChange={(e) => setForm({ ...form, modeTransport: e.target.value })}>
                    {MODES_TRANSPORT.map((m) => (
                      <option key={m.value} value={m.value}>{m.label}</option>
                    ))}
                  </select>
                  {draftFieldErrors.modeTransport && <span className="notes-frais-field-error">{draftFieldErrors.modeTransport}</span>}
                </div>
                {form.modeTransport === 'VOITURE_PERSONNELLE' && (
                  <div className="notes-frais-form-group">
                    <label>Distance (km)</label>
                    <input type="number" min="0" step="0.1" value={form.kilometres || ''} onChange={(e) => setForm({ ...form, kilometres: parseFloat(e.target.value) || 0 })} />
                    {draftFieldErrors.kilometres && <span className="notes-frais-field-error">{draftFieldErrors.kilometres}</span>}
                  </div>
                )}
              </>
            )}
            {form.categorie === 'HEBERGEMENT' && (
              <div className="notes-frais-form-group">
                <label>Ville</label>
                <input
                  type="text"
                  value={form.ville ?? ''}
                  onChange={(e) => { setForm({ ...form, ville: e.target.value }); setDraftFieldErrors((p) => ({ ...p, ville: '' })) }}
                  placeholder="Ex: Casablanca"
                  className={draftFieldErrors.ville ? 'notes-frais-input-error' : ''}
                />
                {draftFieldErrors.ville && <span className="notes-frais-field-error">{draftFieldErrors.ville}</span>}
              </div>
            )}
            {draftFieldErrors.justificatif && <span className="notes-frais-field-error">{draftFieldErrors.justificatif}</span>}
            <div className="notes-frais-form-group">
              <label>Description</label>
              <textarea value={form.description ?? ''} onChange={(e) => setForm({ ...form, description: e.target.value })} rows={2} />
            </div>
            <div className="notes-frais-form-group">
              <label>Justificatif (optionnel selon type)</label>
              <input type="file" accept=".pdf,.jpg,.jpeg,.png" onChange={(e) => setForm({ ...form, justificatif: e.target.files?.[0] })} />
            </div>
            <button type="button" className="notes-frais-btn-submit" onClick={addDraftNote}>
              + Ajouter une autre note
            </button>
            <p className="notes-frais-form-desc">{dossierNotesDraft.length} note(s) prêtes dans le dossier</p>
            {dossierFieldErrors.notesCount && <span className="notes-frais-field-error">{dossierFieldErrors.notesCount}</span>}
            {dossierNotesDraft.length > 0 && (
              <div className="notes-frais-table-wrap">
                <table className="notes-frais-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Catégorie</th>
                      <th>Montant</th>
                      <th>Ville</th>
                      <th>Transport</th>
                    </tr>
                  </thead>
                  <tbody>
                    {dossierNotesDraft.map((n, idx) => (
                      <tr key={`${n.categorie}-${idx}`}>
                        <td>{idx + 1}</td>
                        <td>{n.categorie}</td>
                        <td>{Number(n.montant).toLocaleString('fr-FR', { minimumFractionDigits: 2 })} MAD</td>
                        <td>{n.ville || '-'}</td>
                        <td>{n.modeTransport || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <button type="button" className="notes-frais-btn-submit" disabled={submitting} onClick={handleSubmitDossier}>
              Terminer et soumettre le dossier
            </button>
          </div>
        )}
      </section>

      {/* Demande en cours */}
      {firstEncours && (
        <section className="notes-frais-card">
          <h2>Demande en cours</h2>
          <p className="notes-frais-ref">
            Réf: {firstEncours.reference} • Soumis le {firstEncours.dateSoumission}
          </p>
          <span className={`notes-frais-phase-tag ${STATUT_STYLE[firstEncours.statut] ?? ''}`}>
            Phase: {firstEncours.phaseActuelle}
          </span>
          {firstEncours.suivi && firstEncours.suivi.length >= 3 && (
            <div className="notes-frais-workflow">
              <div className={`notes-frais-workflow-step ${firstEncours.suivi[0].statut === 'VALIDE' ? 'done' : ''}`}>
                <span className="step-icon">{firstEncours.suivi[0].statut === 'VALIDE' ? <CheckCircle size={20} /> : <Clock size={20} />}</span>
                <span>{firstEncours.suivi[0].label}</span>
              </div>
              <div className="notes-frais-workflow-arrow" />
              <div className={`notes-frais-workflow-step ${firstEncours.suivi[1].statut === 'VALIDE' ? 'done' : firstEncours.suivi[1].statut === 'EN_ATTENTE' ? 'current' : ''}`}>
                <span className="step-icon">{firstEncours.suivi[1].statut === 'VALIDE' ? <CheckCircle size={20} /> : <Clock size={20} />}</span>
                <span>{firstEncours.suivi[1].label}</span>
              </div>
              <div className="notes-frais-workflow-arrow" />
              <div className={`notes-frais-workflow-step ${firstEncours.suivi[2].statut === 'VALIDE' ? 'done' : firstEncours.suivi[2].statut === 'EN_ATTENTE' ? 'current' : ''}`}>
                <span className="step-icon">{firstEncours.suivi[2].statut === 'VALIDE' ? <CheckCircle size={20} /> : <Clock size={20} />}</span>
                <span>{firstEncours.suivi[2].label}</span>
              </div>
            </div>
          )}
        </section>
      )}

      {/* Historique global */}
      <section className="notes-frais-card">
        <div className="notes-frais-histoire-header">
          <h2><Clock size={20} /> Historique global (notes + dossiers)</h2>
          <button type="button" className="notes-frais-btn-export" onClick={handleExportPdf} disabled={exporting}>
            <Download size={16} /> Exporter PDF
          </button>
        </div>
        <div className="notes-frais-table-wrap">
          <table className="notes-frais-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Type</th>
                <th>Référence / Description</th>
                <th>Catégorie / Dossier</th>
                <th>Montant total</th>
                <th>Statut</th>
                <th>Remboursé le</th>
              </tr>
            </thead>
            <tbody>
              {combinedHistoriqueDisplay.map((item) => {
                if (item.kind === 'NOTE') {
                  const d = item.note
                  return (
                    <tr key={`note-h-${d.id}`}>
                      <td>{d.dateSoumission}</td>
                      <td><span className="notes-frais-badge-cat">Note simple</span></td>
                      <td>{d.reference}</td>
                      <td><span className="notes-frais-badge-cat">{d.categorieLabel}</span></td>
                      <td>{Number(d.montant).toLocaleString('fr-FR', { minimumFractionDigits: 2 })} MAD</td>
                      <td>
                        <span className={`notes-frais-badge-statut ${STATUT_STYLE[d.statut] ?? ''}`}>
                          {d.statutLabel}
                        </span>
                      </td>
                      <td>{d.dateRemboursement}</td>
                    </tr>
                  )
                }
                const d = item.dossier
                return (
                  <tr key={`dossier-h-${d.id}`}>
                    <td>{d.dateSoumission || d.dateCreation}</td>
                    <td><span className="notes-frais-badge-cat">Dossier</span></td>
                    <td>
                      <button type="button" className="notes-frais-link-tout" onClick={() => openDossierDetails(d.id)}>
                        {d.titre}
                      </button>
                    </td>
                    <td>Dossier de frais</td>
                    <td>{Number(d.montantTotal).toLocaleString('fr-FR', { minimumFractionDigits: 2 })} MAD</td>
                    <td>
                      <span className={`notes-frais-badge-statut ${STATUT_STYLE[d.statut] ?? ''}`}>
                        {d.statutLabel}
                      </span>
                    </td>
                    <td>—</td>
                  </tr>
                )
              })}
              {combinedHistoriqueDisplay.length === 0 && (
                <tr>
                  <td colSpan={7}>Aucune demande dans l’historique</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        {combinedHistorique.length > 10 && (
          <button type="button" className="notes-frais-link-tout" onClick={() => setShowAllHistorique((v) => !v)}>
            {showAllHistorique ? 'Réduire l\'historique' : "Voir tout l'historique"} &gt;
          </button>
        )}
      </section>

      {/* Rappel politique */}
      <div className="notes-frais-rappel">
        <Info size={20} />
        <p>Les justificatifs doivent être soumis dans les 30 jours suivant la dépense pour être éligibles au remboursement.</p>
      </div>
      </>
      )}
      {selectedDossier && (
        <div className="notes-frais-modal-backdrop" onClick={() => setSelectedDossier(null)}>
          <div className="notes-frais-modal" onClick={(e) => e.stopPropagation()}>
            <h3>{selectedDossier.titre}</h3>
            <p className="notes-frais-form-desc">Période : {selectedDossier.dateDebut} - {selectedDossier.dateFin}</p>
            <div className="notes-frais-table-wrap">
              <table className="notes-frais-table">
                <thead>
                  <tr>
                    <th>Référence</th>
                    <th>Catégorie</th>
                    <th>Montant</th>
                    <th>Statut</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedDossier.notes.map((n) => (
                    <tr key={n.id}>
                      <td>{n.reference}</td>
                      <td>{n.categorieLabel}</td>
                      <td>{Number(n.montant).toLocaleString('fr-FR', { minimumFractionDigits: 2 })} MAD</td>
                      <td><span className={`notes-frais-badge-statut ${STATUT_STYLE[n.statut] ?? ''}`}>{n.statutLabel}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <button type="button" className="notes-frais-btn-submit" onClick={() => setSelectedDossier(null)}>Fermer</button>
          </div>
        </div>
      )}
    </div>
  )
}
