import { useState, useEffect, useCallback } from 'react'
import { useAuth } from '../../../context/AuthContext'
import { Settings, Users, RefreshCw, Save } from 'lucide-react'
import * as api from '../../../api/conges'
import { apiFetch } from '../../../api/client'
import type { ParametresCongesDto, SoldeDepartementDto } from '../../../api/conges'
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

  useEffect(() => { loadParametres() }, [loadParametres])
  useEffect(() => { loadUsers() }, [loadUsers])
  useEffect(() => { loadSoldes() }, [loadSoldes])

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
    </div>
  )
}
