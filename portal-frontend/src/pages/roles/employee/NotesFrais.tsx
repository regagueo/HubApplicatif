import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../../context/AuthContext'
import { Receipt, Plus, Clock, CheckCircle, FileUp, Download, Info } from 'lucide-react'
import * as api from '../../../api/frais'
import type { EncoursFraisDto, DemandeFraisDto, CreateDemandeFraisRequest } from '../../../api/frais'
import './NotesFrais.css'

const CATEGORIES = [
  { value: 'TRANSPORT', label: 'Transport' },
  { value: 'REPAS', label: 'Repas' },
  { value: 'HEBERGEMENT', label: 'Hébergement' },
  { value: 'FOURNITURES', label: 'Fournitures' },
  { value: 'AUTRE', label: 'Autre' }
]

const STATUT_STYLE: Record<string, string> = {
  EN_ATTENTE_MANAGER: 'statut-attente',
  EN_ATTENTE_COMPTABILITE: 'statut-attente',
  VALIDE: 'statut-valide',
  REFUSE: 'statut-refuse',
  REMBOURSE: 'statut-paye'
}

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
    description: '',
    justificatif: undefined
  })
  const [dragOver, setDragOver] = useState(false)
  const [exporting, setExporting] = useState(false)
  const [showAllHistorique, setShowAllHistorique] = useState(false)

  const employeeId = user?.id ?? 0
  const historiqueDisplay = showAllHistorique ? historique : historique.slice(0, 10)

  const load = useCallback(async () => {
    if (!employeeId) return
    setLoading(true)
    setError(null)
    try {
      const [enc, hist] = await Promise.all([
        api.getEncours(employeeId),
        api.getHistorique(employeeId)
      ])
      setEncours(enc)
      setHistorique(hist)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Erreur')
      setEncours({ montantEncours: 0, demandesEncours: [] })
      setHistorique([])
    } finally {
      setLoading(false)
    }
  }, [employeeId])

  useEffect(() => {
    load()
  }, [load])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!employeeId || form.montant <= 0) return
    setSubmitting(true)
    setError(null)
    try {
      await api.createDemande(employeeId, form)
      setForm({ montant: 0, categorie: 'TRANSPORT', description: '', justificatif: undefined })
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

  const demandesEncours = encours?.demandesEncours ?? []
  const firstEncours = demandesEncours[0]

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

      {/* Nouvelle demande */}
      <section className="notes-frais-card notes-frais-form-card">
        <h2><Plus size={20} /> Nouvelle Demande</h2>
        <p className="notes-frais-form-desc">Remplissez les détails de votre dépense professionnelle.</p>
        <form onSubmit={handleSubmit} className="notes-frais-form">
          <div className="notes-frais-form-row">
            <div className="notes-frais-form-group">
              <label>Montant (MAD)</label>
              <input
                type="number"
                min="0"
                step="0.01"
                value={form.montant || ''}
                onChange={(e) => setForm({ ...form, montant: parseFloat(e.target.value) || 0 })}
                placeholder="MAD 0.00"
                required
              />
            </div>
            <div className="notes-frais-form-group">
              <label>Catégorie</label>
              <select
                value={form.categorie}
                onChange={(e) => setForm({ ...form, categorie: e.target.value })}
              >
                {CATEGORIES.map((c) => (
                  <option key={c.value} value={c.value}>{c.label}</option>
                ))}
              </select>
            </div>
          </div>
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
                onChange={(e) => setForm({ ...form, justificatif: e.target.files?.[0] })}
                className="notes-frais-upload-input"
              />
              {form.justificatif && (
                <p className="notes-frais-upload-file">{form.justificatif.name}</p>
              )}
            </div>
          </div>
          <button type="submit" className="notes-frais-btn-submit" disabled={submitting}>
            <Receipt size={18} /> Soumettre la Note de Frais
          </button>
        </form>
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

      {/* Historique */}
      <section className="notes-frais-card">
        <div className="notes-frais-histoire-header">
          <h2><Clock size={20} /> Historique des remboursements</h2>
          <button type="button" className="notes-frais-btn-export" onClick={handleExportPdf} disabled={exporting}>
            <Download size={16} /> Exporter PDF
          </button>
        </div>
        <div className="notes-frais-table-wrap">
          <table className="notes-frais-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Description</th>
                <th>Catégorie</th>
                <th>Montant</th>
                <th>Statut</th>
                <th>Remboursé le</th>
              </tr>
            </thead>
            <tbody>
              {historiqueDisplay.map((d) => (
                <tr key={d.id}>
                  <td>{d.dateSoumission}</td>
                  <td>{d.description || d.reference}</td>
                  <td><span className="notes-frais-badge-cat">{d.categorieLabel}</span></td>
                  <td>{Number(d.montant).toLocaleString('fr-FR', { minimumFractionDigits: 2 })} MAD</td>
                  <td>
                    <span className={`notes-frais-badge-statut ${STATUT_STYLE[d.statut] ?? ''}`}>
                      {d.statutLabel}
                    </span>
                  </td>
                  <td>{d.dateRemboursement}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {historique.length > 10 && (
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
    </div>
  )
}
