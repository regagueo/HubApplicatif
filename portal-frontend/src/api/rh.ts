/**
 * API RH - annuaire, indicateurs, collaborateurs (PostgreSQL)
 */

import { apiFetch } from './client'

const BASE = '/rh'

export async function getRhHealth() {
  const res = await apiFetch(`${BASE}/health`)
  if (!res.ok) throw new Error('Erreur rh-service')
  return res.json()
}
