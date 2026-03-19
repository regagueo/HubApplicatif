/**
 * API Files - documents, pièces jointes (MongoDB)
 */

import { apiFetch } from './client'

const BASE = '/files'

export async function getFilesHealth() {
  const res = await apiFetch(`${BASE}/health`)
  if (!res.ok) throw new Error('Erreur file-service')
  return res.json()
}

export async function listFiles() {
  const res = await apiFetch(BASE)
  if (!res.ok) throw new Error('Erreur chargement fichiers')
  return res.json()
}
