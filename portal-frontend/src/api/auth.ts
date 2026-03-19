import { apiFetch } from './client'

export interface UserPeek {
  id: number
  username: string
}

export async function fetchUsersForChat(): Promise<UserPeek[]> {
  const res = await apiFetch('/auth/users/peers')
  if (!res.ok) throw new Error('Erreur lors du chargement des utilisateurs')
  const list = await res.json()
  return Array.isArray(list) ? list : []
}
