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

async function readErrorMessage(res: Response, fallback: string): Promise<string> {
  try {
    const txt = await res.text()
    const httpInfo = `HTTP ${res.status}${res.statusText ? ` ${res.statusText}` : ''}`
    if (!txt) return `${fallback} (${httpInfo})`
    try {
      const json = JSON.parse(txt) as { message?: string; error?: string }
      const msg = json.message || json.error || txt
      return msg && msg.trim().length > 0 ? msg : `${fallback} (${httpInfo})`
    } catch {
      return txt && txt.trim().length > 0 ? txt : `${fallback} (${httpInfo})`
    }
  } catch {
    return fallback
  }
}

export interface EncoursFraisDto {
  montantEncours: number
  demandesEncours: DemandeFraisDto[]
}

export interface DemandeFraisDto {
  id: number
  employeeId: number
  dossierId?: number
  reference: string
  montant: number
  categorie: string
  categorieLabel: string
  modeTransport?: string
  kilometres?: number
  ville?: string
  anneesExperience?: number
  description: string
  statut: string
  statutLabel: string
  dateSoumission: string
  dateSoumissionIso?: string
  dateRemboursement: string
  phaseActuelle: string
  managerNom?: string
  rhNom?: string
  suivi?: { code: string; label: string; statut: string; date: string }[]
}

export interface CreateDemandeFraisRequest {
  montant: number
  categorie: string
  modeTransport?: string
  kilometres?: number
  ville?: string
  anneesExperience?: number
  description?: string
  justificatif?: File
}

export interface PlafondFraisDto {
  id: number
  transportTarifKm: number
  taxiForfaitSansJustificatif: number
  repasMoins3Ans: number
  repas3AnsEtPlus: number
  repas3AnsEtPlusMax: number
  hebergementCasablanca: number
  hebergementAutresVilles: number
  perDiemActif: boolean
  perDiemGlobal: number
  dateMiseAJour: string
  modifiePar?: string
}

export interface DossierFraisDto {
  id: number
  employeeId: number
  titre: string
  dateDebut: string
  dateFin: string
  statut: string
  statutLabel: string
  dateCreation: string
  dateCreationIso?: string
  dateSoumission: string
  dateSoumissionIso?: string
  managerNom?: string
  rhNom?: string
  montantTotal: number
  notes: DemandeFraisDto[]
}

export async function getEncours(employeeId: number): Promise<EncoursFraisDto> {
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/encours`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error(await readErrorMessage(res, 'Erreur chargement encours'))
  return res.json()
}

export async function getHistorique(employeeId: number): Promise<DemandeFraisDto[]> {
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/historique`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error(await readErrorMessage(res, 'Erreur chargement historique'))
  return res.json()
}

export async function createDemande(employeeId: number, body: CreateDemandeFraisRequest): Promise<DemandeFraisDto> {
  const form = new FormData()
  form.append('montant', String(body.montant))
  form.append('categorie', body.categorie)
  if (body.modeTransport) form.append('modeTransport', body.modeTransport)
  if (body.kilometres != null) form.append('kilometres', String(body.kilometres))
  if (body.ville) form.append('ville', body.ville)
  if (body.anneesExperience != null) form.append('anneesExperience', String(body.anneesExperience))
  if (body.description) form.append('description', body.description)
  if (body.justificatif) form.append('justificatif', body.justificatif)
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/demande`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: form
  })
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, 'Erreur création demande'))
  }
  return res.json()
}

export async function exportPdf(employeeId: number): Promise<Blob> {
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/historique/export-pdf`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Erreur export PDF')
  return res.blob()
}

export async function getDemandesValidation(etape: 'MANAGER' | 'RH' = 'MANAGER'): Promise<DemandeFraisDto[]> {
  const res = await fetch(`${API_BASE}${BASE}/demandes/validation?etape=${etape}`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Erreur chargement demandes à valider')
  return res.json()
}

export async function validerDemandeParManager(demandeId: number, validateurNom?: string): Promise<DemandeFraisDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/demandes/${demandeId}/valider-manager${q}`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error('Erreur validation manager')
  return res.json()
}

export async function refuserDemandeParManager(demandeId: number, validateurNom?: string): Promise<DemandeFraisDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/demandes/${demandeId}/refuser-manager${q}`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error('Erreur refus manager')
  return res.json()
}

export async function validerDemandeParRh(demandeId: number, validateurNom?: string): Promise<DemandeFraisDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/demandes/${demandeId}/valider-rh${q}`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error('Erreur validation RH')
  return res.json()
}

export async function refuserDemandeParRh(demandeId: number, validateurNom?: string): Promise<DemandeFraisDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/demandes/${demandeId}/refuser-rh${q}`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error('Erreur refus RH')
  return res.json()
}

export async function getPlafondsFrais(): Promise<PlafondFraisDto> {
  const res = await fetch(`${API_BASE}${BASE}/plafonds`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Erreur chargement plafonds frais')
  return res.json()
}

export async function updatePlafondsFrais(payload: PlafondFraisDto, modifiePar?: string): Promise<PlafondFraisDto> {
  const q = modifiePar ? `?modifiePar=${encodeURIComponent(modifiePar)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/plafonds${q}`, {
    method: 'PUT',
    headers: {
      ...getAuthHeaders(),
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(payload)
  })
  if (!res.ok) throw new Error('Erreur mise à jour plafonds frais')
  return res.json()
}

export async function createDossier(employeeId: number, titre: string, dateDebut: string, dateFin: string): Promise<DossierFraisDto> {
  const params = new URLSearchParams()
  params.set('titre', titre)
  params.set('dateDebut', dateDebut)
  params.set('dateFin', dateFin)
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/dossiers?${params.toString()}`, {
    method: 'POST',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error(await readErrorMessage(res, 'Erreur création dossier'))
  return res.json()
}

export async function addNoteToDossier(employeeId: number, dossierId: number, body: CreateDemandeFraisRequest): Promise<DemandeFraisDto> {
  const form = new FormData()
  form.append('montant', String(body.montant))
  form.append('categorie', body.categorie)
  if (body.modeTransport) form.append('modeTransport', body.modeTransport)
  if (body.kilometres != null) form.append('kilometres', String(body.kilometres))
  if (body.ville) form.append('ville', body.ville)
  if (body.anneesExperience != null) form.append('anneesExperience', String(body.anneesExperience))
  if (body.description) form.append('description', body.description)
  if (body.justificatif) form.append('justificatif', body.justificatif)
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/dossiers/${dossierId}/notes`, {
    method: 'POST',
    headers: getAuthHeaders(),
    body: form
  })
  if (!res.ok) {
    throw new Error(await readErrorMessage(res, 'Erreur ajout note au dossier'))
  }
  return res.json()
}

export async function submitDossier(employeeId: number, dossierId: number): Promise<DossierFraisDto> {
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/dossiers/${dossierId}/soumettre`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error(await readErrorMessage(res, 'Erreur soumission dossier'))
  return res.json()
}

export async function getDossiers(employeeId: number): Promise<DossierFraisDto[]> {
  const res = await fetch(`${API_BASE}${BASE}/${employeeId}/dossiers`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error(await readErrorMessage(res, 'Erreur chargement dossiers'))
  return res.json()
}

export async function getDossier(dossierId: number): Promise<DossierFraisDto> {
  const res = await fetch(`${API_BASE}${BASE}/dossiers/${dossierId}`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Erreur chargement dossier')
  return res.json()
}

export async function getDossiersValidation(etape: 'MANAGER' | 'RH' = 'MANAGER'): Promise<DossierFraisDto[]> {
  const res = await fetch(`${API_BASE}${BASE}/dossiers/validation?etape=${etape}`, { headers: getAuthHeaders() })
  if (!res.ok) throw new Error('Erreur chargement dossiers à valider')
  return res.json()
}

export async function validerDossierParManager(dossierId: number, validateurNom?: string): Promise<DossierFraisDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/dossiers/${dossierId}/valider-manager${q}`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error('Erreur validation dossier manager')
  return res.json()
}

export async function refuserDossierParManager(dossierId: number, validateurNom?: string): Promise<DossierFraisDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/dossiers/${dossierId}/refuser-manager${q}`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error('Erreur refus dossier manager')
  return res.json()
}

export async function validerDossierParRh(dossierId: number, validateurNom?: string): Promise<DossierFraisDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/dossiers/${dossierId}/valider-rh${q}`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error('Erreur validation dossier RH')
  return res.json()
}

export async function refuserDossierParRh(dossierId: number, validateurNom?: string): Promise<DossierFraisDto> {
  const q = validateurNom ? `?validateurNom=${encodeURIComponent(validateurNom)}` : ''
  const res = await fetch(`${API_BASE}${BASE}/dossiers/${dossierId}/refuser-rh${q}`, {
    method: 'PUT',
    headers: getAuthHeaders()
  })
  if (!res.ok) throw new Error('Erreur refus dossier RH')
  return res.json()
}
