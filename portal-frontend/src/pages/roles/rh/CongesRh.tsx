import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../../context/AuthContext'
import { Settings, Users, RefreshCw, Save, CalendarPlus, Trash2 } from 'lucide-react'
import * as api from '../../../api/conges'
import { apiFetch } from '../../../api/client'
import type { ParametresCongesDto, SoldeDepartementDto, JourFerieDto } from '../../../api/conges'
import './CongesRh.css'

interface UserInfo {
  id: number
  username: string
  firstName?: string
  lastName?: string
}

interface UsersPage {
  content: UserInfo[]
  totalElements: number
}

const ANNEES = [2026, 2025, 2024]

export default function CongesRh() {
  const { user } = useAuth()
  const [parametres, setParametres] = useState<ParametresCongesDto | null>(null)
  const [editVal, setEditVal] = useState('')
  const [saving, setSaving] = useState(false)
  const [msgParametres, setMsgParametres] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)

  const [soldes, setSoldes] = useState<SoldeDepartementDto[]>([])
  const [usersMap, setUsersMap] = useState<Record<number, string>>({})
  const [anneeSoldes, setAnneeSoldes] = useState(new Date().getFullYear())
  const [loadingSoldes, setLoadingSoldes] = useState(false)
  const [filterNom, setFilterNom] = useState('')
  const [anneeJoursFeries, setAnneeJoursFeries] = useState(new Date().getFullYear())
  const [joursFeries, setJoursFeries] = useState<JourFerieDto[]>([])
  const [loadingJoursFeries, setLoadingJoursFeries] = useState(false)
  const [syncingJoursFeries, setSyncingJoursFeries] = useState(false)
  const [newJourFerieDate, setNewJourFerieDate] = useState('')
  const [newJourFerieNom, setNewJourFerieNom] = useState('')
  const [msgJoursFeries, setMsgJoursFeries] = useState<{ type: 'ok' | 'err'; text: string } | null>(null)

  const loadParametres = useCallback(async () => {
    try {
      const p = await api.getParametresConges()
      setParametres(p)
      setEditVal(String(p.valeurAcquisitionMensuelle))
    } catch {
      setMsgParametres({ type: 'err', text: 'Erreur chargement paramètres' })
    }
  }, [])

  const loadUsers = useCallback(async () => {
    try {
      const res = await apiFetch('/auth/users?size=200')
      if (res.ok) {
        const data: UsersPage = await res.json()
        const map: Record<number, string> = {}
        data.content.forEach((u) => {
          const name = u.firstName && u.lastName ? `${u.firstName} ${u.lastName}` : u.username
          map[u.id] = name
        })
        setUsersMap(map)
      }
    } catch { /* silent */ }
  }, [])

  const loadSoldes = useCallback(async () => {
    setLoadingSoldes(true)
    try {
      const data = await api.getSoldesParDepartement(anneeSoldes)
      setSoldes(data)
    } catch {
      setSoldes([])
    } finally {
      setLoadingSoldes(false)
    }
  }, [anneeSoldes])

  const loadJoursFeries = useCallback(async () => {
    setLoadingJoursFeries(true)
    try {
      const data = await api.getJoursFeries(anneeJoursFeries)
      setJoursFeries(data)
    } catch {
      setJoursFeries([])
      setMsgJoursFeries({ type: 'err', text: 'Erreur chargement jours fériés' })
    } finally {
      setLoadingJoursFeries(false)
    }
  }, [anneeJoursFeries])

  useEffect(() => { loadParametres() }, [loadParametres])
  useEffect(() => { loadUsers() }, [loadUsers])
  useEffect(() => { loadSoldes() }, [loadSoldes])
  useEffect(() => { loadJoursFeries() }, [loadJoursFeries])

  const handleSaveParametres = async () => {
    const val = parseFloat(editVal)
    if (isNaN(val) || val <= 0) {
      setMsgParametres({ type: 'err', text: 'Valeur invalide' })
      return
    }
    setSaving(true)
    setMsgParametres(null)
    try {
      const userName = user?.firstName && user?.lastName
        ? `${user.firstName} ${user.lastName}`
        : user?.username ?? 'RH'
      const updated = await api.updateParametresConges(val, userName)
      setParametres(updated)
      setEditVal(String(updated.valeurAcquisitionMensuelle))
      setMsgParametres({ type: 'ok', text: 'Paramètre mis à jour avec succès' })
    } catch (err) {
      setMsgParametres({ type: 'err', text: err instanceof Error ? err.message : 'Erreur' })
    } finally {
      setSaving(false)
    }
  }

  const employeeIds = [...new Set(soldes.map((s) => s.employeeId))]
  const filteredEmployeeIds = filterNom.trim()
    ? employeeIds.filter((id) => {
        const nom = usersMap[id] ?? `Employé #${id}`
        return nom.toLowerCase().includes(filterNom.toLowerCase())
      })
    : employeeIds

  const handleSyncJoursFeries = async () => {
    setSyncingJoursFeries(true)
    setMsgJoursFeries(null)
    try {
      await api.syncJoursFeries(anneeJoursFeries)
      await loadJoursFeries()
      setMsgJoursFeries({ type: 'ok', text: 'Synchronisation Nager effectuée avec succès' })
    } catch (err) {
      setMsgJoursFeries({ type: 'err', text: err instanceof Error ? err.message : 'Erreur synchronisation' })
    } finally {
      setSyncingJoursFeries(false)
    }
  }

  const handleAddJourFerie = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newJourFerieDate || !newJourFerieNom.trim()) {
      setMsgJoursFeries({ type: 'err', text: 'Veuillez renseigner la date et le nom.' })
      return
    }
    setMsgJoursFeries(null)
    try {
      await api.addJourFerie(newJourFerieDate, newJourFerieNom.trim())
      setNewJourFerieDate('')
      setNewJourFerieNom('')
      await loadJoursFeries()
      setMsgJoursFeries({ type: 'ok', text: 'Jour férié ajouté avec succès' })
    } catch (err) {
      setMsgJoursFeries({ type: 'err', text: err instanceof Error ? err.message : 'Erreur ajout jour férié' })
    }
  }

  const handleDeleteJourFerie = async (id: number) => {
    if (!window.confirm('Supprimer ce jour férié ?')) return
    setMsgJoursFeries(null)
    try {
      await api.deleteJourFerie(id)
      await loadJoursFeries()
      setMsgJoursFeries({ type: 'ok', text: 'Jour férié supprimé avec succès' })
    } catch (err) {
      setMsgJoursFeries({ type: 'err', text: err instanceof Error ? err.message : 'Erreur suppression jour férié' })
    }
  }

  return (
    <div className="conges-rh-page">
      <header className="conges-rh-header">
        <h1>Gestion des congés — Administration RH</h1>
        <p className="conges-rh-subtitle">Paramétrez les règles d&apos;acquisition et consultez les soldes de tous les employés.</p>
      </header>

      <section className="conges-rh-section conges-rh-params">
        <h2 className="conges-rh-section-title">
          <Settings size={20} />
          Paramétrage acquisition mensuelle
        </h2>
        <div className="conges-rh-params-form">
          <div className="conges-rh-field">
            <label>Jours acquis par mois</label>
            <input
              type="number"
              step="0.1"
              min="0"
              value={editVal}
              onChange={(e) => setEditVal(e.target.value)}
            />
          </div>
          {parametres && (
            <p className="conges-rh-params-info">
              Dernière mise à jour : {new Date(parametres.dateMiseAJour).toLocaleDateString('fr-FR')}
              {parametres.creePar ? ` par ${parametres.creePar}` : ''}
            </p>
          )}
          <button
            className="conges-rh-btn-save"
            onClick={handleSaveParametres}
            disabled={saving}
          >
            <Save size={16} />
            {saving ? 'Enregistrement...' : 'Enregistrer'}
          </button>
          {msgParametres && (
            <p className={`conges-rh-msg ${msgParametres.type === 'ok' ? 'conges-rh-msg-ok' : 'conges-rh-msg-err'}`}>
              {msgParametres.text}
            </p>
          )}
        </div>
      </section>

      <section className="conges-rh-section">
        <h2 className="conges-rh-section-title">
          <Users size={20} />
          Soldes congés — Tous les employés
        </h2>
        <div className="conges-rh-soldes-controls">
          <div className="conges-rh-field">
            <label>Année</label>
            <select value={anneeSoldes} onChange={(e) => setAnneeSoldes(Number(e.target.value))}>
              {ANNEES.map((y) => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>
          </div>
          <div className="conges-rh-field">
            <label>Rechercher</label>
            <input
              type="text"
              placeholder="Nom de l'employé..."
              value={filterNom}
              onChange={(e) => setFilterNom(e.target.value)}
            />
          </div>
          <button className="conges-rh-btn-refresh" onClick={loadSoldes}>
            <RefreshCw size={16} />
            Actualiser
          </button>
        </div>

        {loadingSoldes ? (
          <p className="conges-rh-loading">Chargement...</p>
        ) : filteredEmployeeIds.length === 0 ? (
          <p className="conges-rh-empty">Aucun solde trouvé pour cette année.</p>
        ) : (
          <div className="conges-rh-table-wrap">
            <table className="conges-rh-table">
              <thead>
                <tr>
                  <th>Employé</th>
                  <th>Type</th>
                  <th>Jours Total</th>
                  <th>Jours Pris</th>
                  <th>Jours Restants</th>
                </tr>
              </thead>
              <tbody>
                {filteredEmployeeIds.map((empId) => {
                  const empSoldes = soldes.filter((s) => s.employeeId === empId)
                  const nom = usersMap[empId] ?? `Employé #${empId}`
                  return empSoldes.map((s, idx) => (
                    <tr key={`${empId}-${s.type}`}>
                      {idx === 0 && (
                        <td rowSpan={empSoldes.length} className="conges-rh-td-name">
                          {nom}
                        </td>
                      )}
                      <td>{s.label || s.type}</td>
                      <td>{s.joursTotal}</td>
                      <td>{s.joursPris}</td>
                      <td className={s.joursRestants <= 0 ? 'conges-rh-val-zero' : ''}>
                        {s.joursRestants}
                      </td>
                    </tr>
                  ))
                })}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <section className="conges-rh-section">
        <h2 className="conges-rh-section-title">
          <CalendarPlus size={20} />
          Jours fériés (Maroc)
        </h2>
        <div className="conges-rh-feries-controls">
          <div className="conges-rh-field">
            <label>Année</label>
            <select value={anneeJoursFeries} onChange={(e) => setAnneeJoursFeries(Number(e.target.value))}>
              {ANNEES.map((y) => (
                <option key={y} value={y}>{y}</option>
              ))}
            </select>
          </div>
          <button className="conges-rh-btn-refresh" onClick={handleSyncJoursFeries} disabled={syncingJoursFeries}>
            <RefreshCw size={16} />
            {syncingJoursFeries ? 'Synchronisation...' : 'Synchroniser Nager'}
          </button>
        </div>

        <form className="conges-rh-feries-add-form" onSubmit={handleAddJourFerie}>
          <div className="conges-rh-field">
            <label>Date</label>
            <input type="date" value={newJourFerieDate} onChange={(e) => setNewJourFerieDate(e.target.value)} />
          </div>
          <div className="conges-rh-field">
            <label>Nom</label>
            <input
              type="text"
              placeholder="Nom du jour férié"
              value={newJourFerieNom}
              onChange={(e) => setNewJourFerieNom(e.target.value)}
            />
          </div>
          <button className="conges-rh-btn-save" type="submit">Ajouter</button>
        </form>

        {msgJoursFeries && (
          <p className={`conges-rh-msg ${msgJoursFeries.type === 'ok' ? 'conges-rh-msg-ok' : 'conges-rh-msg-err'}`}>
            {msgJoursFeries.text}
          </p>
        )}

        {loadingJoursFeries ? (
          <p className="conges-rh-loading">Chargement...</p>
        ) : joursFeries.length === 0 ? (
          <p className="conges-rh-empty">Aucun jour férié trouvé pour cette année.</p>
        ) : (
          <div className="conges-rh-table-wrap">
            <table className="conges-rh-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Nom</th>
                  <th>Source</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {joursFeries.map((jf) => (
                  <tr key={jf.id}>
                    <td>{new Date(jf.date).toLocaleDateString('fr-FR')}</td>
                    <td>{jf.libelle}</td>
                    <td>{jf.source}</td>
                    <td>
                      <button
                        className="conges-rh-btn-delete"
                        type="button"
                        onClick={() => handleDeleteJourFerie(jf.id)}
                      >
                        <Trash2 size={16} />
                        Supprimer
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  )
}
