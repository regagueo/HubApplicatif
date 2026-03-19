/**
 * API Admin - utilisateurs, rôles, paramètres (PostgreSQL)
 */

import { apiFetch } from './client'

const BASE = '/admin'

export async function getAdminHealth() {
  const res = await apiFetch(`${BASE}/health`)
  if (!res.ok) throw new Error('Erreur admin-service')
  return res.json()
}
