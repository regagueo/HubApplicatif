/**
 * API Employee - Structure pour les appels backend du tableau de bord employé.
 * À brancher lorsque les microservices congés / notes de frais / RH seront disponibles.
 */

import { apiFetch } from './client'

const EMPLOYEE_BASE = '/employee'

/** Demandes de congés - GET /employee/{id}/conges */
export async function fetchConges(employeeId: number) {
  const res = await apiFetch(`${EMPLOYEE_BASE}/${employeeId}/conges`)
  if (!res.ok) throw new Error('Erreur chargement congés')
  return res.json()
}

/** Notes de frais - GET /employee/{id}/notes-frais */
export async function fetchNotesFrais(employeeId: number) {
  const res = await apiFetch(`${EMPLOYEE_BASE}/${employeeId}/notes-frais`)
  if (!res.ok) throw new Error('Erreur chargement notes de frais')
  return res.json()
}

/** Indicateurs personnels - GET /employee/{id}/indicators */
export async function fetchIndicators(employeeId: number) {
  const res = await apiFetch(`${EMPLOYEE_BASE}/${employeeId}/indicators`)
  if (!res.ok) throw new Error('Erreur chargement indicateurs')
  return res.json()
}

/** Alertes / Notifications - GET /employee/{id}/alerts */
export async function fetchAlerts(employeeId: number) {
  const res = await apiFetch(`${EMPLOYEE_BASE}/${employeeId}/alerts`)
  if (!res.ok) throw new Error('Erreur chargement alertes')
  return res.json()
}

/** Collaborateurs / Annuaire - GET /employee/{id}/colleagues ou /rh/annuaire */
export async function fetchColleagues(employeeId?: number) {
  const path = employeeId ? `${EMPLOYEE_BASE}/${employeeId}/colleagues` : '/rh/annuaire'
  const res = await apiFetch(path)
  if (!res.ok) throw new Error('Erreur chargement collaborateurs')
  return res.json()
}
