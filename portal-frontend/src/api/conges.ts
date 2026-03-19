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

export interface JourFerieDto {
  id: number
  date: string
  libelle: string
  source: string
  annee: number
}

export interface CalculJoursOuvresDto {
  joursOuvres: number
  weekEndsExclus: number
  joursFeriesExclus: number
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

export async function getJoursFeries(annee?: number): Promise<JourFerieDto[]> {
  const q = annee != null ? `?annee=${annee}` : ''
  const res = await apiFetch(`${BASE}/jours-feries${q}`)
  if (!res.ok) throw new Error('Erreur chargement jours fériés')
  return res.json()
}

export async function addJourFerie(date: string, libelle: string): Promise<JourFerieDto> {
  const params = new URLSearchParams()
  params.set('date', date)
  params.set('libelle', libelle)
  const res = await apiFetch(`${BASE}/jours-feries?${params.toString()}`, { method: 'POST' })
  if (!res.ok) throw new Error('Erreur ajout jour férié')
  return res.json()
}

export async function deleteJourFerie(id: number): Promise<void> {
  const res = await apiFetch(`${BASE}/jours-feries/${id}`, { method: 'DELETE' })
  if (!res.ok) throw new Error('Erreur suppression jour férié')
}

export async function syncJoursFeries(annee: number): Promise<JourFerieDto[]> {
  const res = await apiFetch(`${BASE}/jours-feries/sync?annee=${annee}`, { method: 'POST' })
  if (!res.ok) throw new Error('Erreur synchronisation Nager')
  return res.json()
}

export async function calculJoursOuvres(dateDebut: string, dateFin: string, periode: string): Promise<CalculJoursOuvresDto> {
  const params = new URLSearchParams()
  params.set('dateDebut', dateDebut)
  params.set('dateFin', dateFin)
  params.set('periode', periode)
  const res = await apiFetch(`${BASE}/calcul-jours-ouvres?${params.toString()}`)
  if (!res.ok) throw new Error('Erreur calcul jours ouvrés')
  return res.json()
}

export async function getDemandesEnAttente(etape: 'MANAGER' | 'RH' = 'MANAGER'): Promise<DemandeCongesDto[]> {
  const res = await apiFetch(`${BASE}/demandes/en-attente?etape=${etape}`)
  if (!res.ok) throw new Error('Erreur chargement des demandes à valider')
  return res.json()
}

export async function getDemandesValidation(etape: 'MANAGER' | 'RH' = 'MANAGER'): Promise<DemandeCongesDto[]> {
  const res = await apiFetch(`${BASE}/demandes/validation?etape=${etape}`)
  if (!res.ok) throw new Error('Erreur chargement historique validation')
  return res.json()
}

export async function validerDemandeParManager(demandeId: number, validateurNom?: string): Promise<DemandeCongesDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await apiFetch(`${BASE}/demandes/${demandeId}/valider-manager${q}`, { method: 'PUT' })
  if (!res.ok) throw new Error('Erreur validation manager')
  return res.json()
}

export async function refuserDemandeParManager(demandeId: number, validateurNom?: string): Promise<DemandeCongesDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await apiFetch(`${BASE}/demandes/${demandeId}/refuser-manager${q}`, { method: 'PUT' })
  if (!res.ok) throw new Error('Erreur refus manager')
  return res.json()
}

export async function validerDemandeParRh(demandeId: number, validateurNom?: string): Promise<DemandeCongesDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await apiFetch(`${BASE}/demandes/${demandeId}/valider-rh${q}`, { method: 'PUT' })
  if (!res.ok) throw new Error('Erreur validation RH')
  return res.json()
}

export async function refuserDemandeParRh(demandeId: number, validateurNom?: string): Promise<DemandeCongesDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await apiFetch(`${BASE}/demandes/${demandeId}/refuser-rh${q}`, { method: 'PUT' })
  if (!res.ok) throw new Error('Erreur refus RH')
  return res.json()
}
