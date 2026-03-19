/**
 * API Congés - soldes, demandes, suivi (conges-service)
 */

import { apiFetch } from './client'

const BASE = '/conges'

export interface SoldeCongesDto {
  type: string
  label: string
  jours: number
  joursPris: number
  joursRestants: number
}

export interface DemandeCongesDto {
  id: number
  employeeId: number
  dateDebut: string
  dateFin: string
  motif: string
  motifCode: string
  periode: string
  commentaire?: string
  statut: string
  statutLabel: string
  dateSoumission: string
  dureeJours: number
  validateurNom: string
  suivi?: { etape: string; label: string; statut: string; date: string }[]
}

export interface CreateDemandeRequest {
  dateDebut: string
  dateFin: string
  motif: string
  periode: string
  commentaire?: string
}

export async function getSoldes(employeeId: number): Promise<SoldeCongesDto[]> {
  const res = await apiFetch(`${BASE}/${employeeId}/soldes`)
  if (!res.ok) throw new Error('Erreur chargement soldes')
  return res.json()
}

export async function getDemandes(employeeId: number, annee?: number, statut?: string): Promise<DemandeCongesDto[]> {
  const params = new URLSearchParams()
  if (annee != null) params.set('annee', String(annee))
  if (statut) params.set('statut', statut)
  const q = params.toString()
  const res = await apiFetch(`${BASE}/${employeeId}/demandes${q ? '?' + q : ''}`)
  if (!res.ok) throw new Error('Erreur chargement demandes')
  return res.json()
}

export async function getDemande(demandeId: number): Promise<DemandeCongesDto> {
  const res = await apiFetch(`${BASE}/demandes/${demandeId}`)
  if (!res.ok) throw new Error('Demande introuvable')
  return res.json()
}

export async function getSuivi(demandeId: number): Promise<DemandeCongesDto['suivi']> {
  const res = await apiFetch(`${BASE}/demandes/${demandeId}/suivi`)
  if (!res.ok) throw new Error('Erreur suivi')
  return res.json()
}

export async function createDemande(employeeId: number, body: CreateDemandeRequest): Promise<DemandeCongesDto> {
  const res = await apiFetch(`${BASE}/${employeeId}/demandes`, {
    method: 'POST',
    body: JSON.stringify(body)
  })
  if (!res.ok) {
    const err = await res.text()
    throw new Error(err || 'Erreur création demande')
  }
  return res.json()
}

export async function updateDemande(employeeId: number, demandeId: number, body: CreateDemandeRequest): Promise<DemandeCongesDto> {
  const res = await apiFetch(`${BASE}/${employeeId}/demandes/${demandeId}`, {
    method: 'PUT',
    body: JSON.stringify(body)
  })
  if (!res.ok) throw new Error('Erreur modification')
  return res.json()
}

export async function annulerDemande(employeeId: number, demandeId: number): Promise<void> {
  const res = await apiFetch(`${BASE}/${employeeId}/demandes/${demandeId}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Erreur annulation')
}

export interface HistoriqueSoldeDto {
  id: number
  typeMouvement: string
  valeur: number
  dateMouvement: string
  referenceDemandeId?: number | null
  libelle: string
}

export interface ParametresCongesDto {
  id: number
  valeurAcquisitionMensuelle: number
  dateMiseAJour: string
  creePar: string
}

export interface SoldeDepartementDto {
  employeeId: number
  type: string
  label: string
  joursTotal: number
  joursPris: number
  joursRestants: number
  annee: number
}

export async function getHistoriqueSolde(employeeId: number, annee?: number): Promise<HistoriqueSoldeDto[]> {
  const q = annee != null ? `?annee=${annee}` : ''
  const res = await apiFetch(`${BASE}/${employeeId}/historique-solde${q}`)
  if (!res.ok) throw new Error('Erreur chargement historique solde')
  return res.json()
}

export async function getParametresConges(): Promise<ParametresCongesDto> {
  const res = await apiFetch(`${BASE}/parametres`)
  if (!res.ok) throw new Error('Erreur chargement paramètres congés')
  return res.json()
}

export async function updateParametresConges(valeurAcquisitionMensuelle: number, creePar?: string): Promise<ParametresCongesDto> {
  const params = new URLSearchParams()
  params.set('valeurAcquisitionMensuelle', String(valeurAcquisitionMensuelle))
  if (creePar) params.set('creePar', creePar)
  const res = await apiFetch(`${BASE}/parametres?${params.toString()}`, { method: 'PUT' })
  if (!res.ok) throw new Error('Erreur mise à jour paramètres')
  return res.json()
}

export async function getSoldesParDepartement(annee?: number): Promise<SoldeDepartementDto[]> {
  const q = annee != null ? `?annee=${annee}` : ''
  const res = await apiFetch(`${BASE}/rh/soldes-departement${q}`)
  if (!res.ok) throw new Error('Erreur chargement soldes par département')
  return res.json()
}
