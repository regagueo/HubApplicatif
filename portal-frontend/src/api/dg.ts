/**
 * API DG - tableau de bord global, indicateurs consolidés (PostgreSQL)
 */

import { apiFetch } from './client'

const BASE = '/dg'

export async function getDgHealth() {
  const res = await apiFetch(`${BASE}/health`)
  if (!res.ok) throw new Error('Erreur dg-service')
  return res.json()
}
