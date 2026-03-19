/**
 * API Manager - validation équipe, congés, notes de frais (PostgreSQL)
 */

import { apiFetch } from './client'

const BASE = '/manager'

export async function getManagerHealth() {
  const res = await apiFetch(`${BASE}/health`)
  if (!res.ok) throw new Error('Erreur manager-service')
  return res.json()
}
