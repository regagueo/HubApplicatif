/**
 * API Notes de frais - encours, demandes, historique (frais-service)
 */

const API_BASE = import.meta.env.DEV ? '/api' : (import.meta.env.VITE_API_URL || 'http://localhost:8080')

function getAuthHeaders(): HeadersInit {
  const token = localStorage.getItem('portail_auth_token')
  const h: Record<string, string> = {}
  if (token) h['Authorization'] = `Bearer ${token}`
  return h
}

const BASE = '/frais'

export interface EncoursFraisDto {
  montantEncours: number
  demandesEncours: DemandeFraisDto[]
}

export interface DemandeFraisDto {
  id: number
  employeeId: number
  reference: string
  montant: number
  categorie: string
  categorieLabel: string
  description: string
  statut: string
  statutLabel: string
  dateSoumission: string
  dateRemboursement: string
  phaseActuelle: string
  managerNom?: string
  suivi?: { code: string; label: string; statut: string; date: string }[]
}

export interface CreateDemandeFraisRequest {
  montant: number
  categorie: string
  description?: string
  justificatif?: File
}

export async function getEncours(employeeId: number): Promise<EncoursFraisDto> {
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/encours`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Erreur chargement encours')
  return res.json()
}

export async function getHistorique(employeeId: number): Promise<DemandeFraisDto[]> {
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/historique`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Erreur chargement historique')
  return res.json()
}

export async function createDemande(employeeId: number, body: CreateDemandeFraisRequest): Promise<DemandeFraisDto> {
  const form = new FormData()
  form.append('montant', String(body.montant))
  form.append('categorie', body.categorie)
  if (body.description) form.append('description', body.description)
  if (body.justificatif) form.append('justificatif', body.justificatif)
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/demande`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: form
  })
  if (!res.ok) {
    const err = await res.text()
    throw new Error(err || 'Erreur création demande')
  }
  return res.json()
}

export async function exportPdf(employeeId: number): Promise<Blob> {
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/historique/export-pdf`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Erreur export PDF')
  return res.blob()
}
