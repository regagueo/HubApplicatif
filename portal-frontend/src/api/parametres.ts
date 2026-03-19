/**
 * API Paramètres du compte - profil, préférences, sécurité, notifications
 */

import { apiFetch } from './client'

const BASE = '/parametres'

export interface ProfilDto {
  emailPro?: string
  telephone?: string
  photoUrl?: string
  poste?: string
  departement?: string
  localisation?: string
  typeContrat?: string
}

export interface PreferencesDto {
  language: string
  theme: string
}

export interface SecuriteDto {
  mfaEnabled?: boolean
  passwordChangedAt?: string
}

export interface NotificationsDto {
  emailAlerts?: boolean
  pushEnabled?: boolean
  smsEnabled?: boolean
}

export async function getProfil(): Promise<ProfilDto> {
  const res = await apiFetch(`${BASE}/me/profile`)
  if (!res.ok) throw new Error('Erreur chargement profil')
  return res.json()
}

export async function updateProfil(dto: ProfilDto): Promise<ProfilDto> {
  const res = await apiFetch(`${BASE}/me/profile`, { method: 'PUT', body: JSON.stringify(dto) })
  if (!res.ok) throw new Error('Erreur enregistrement profil')
  return res.json()
}

export async function getPreferences(): Promise<PreferencesDto> {
  const res = await apiFetch(`${BASE}/me/preferences`)
  if (!res.ok) throw new Error('Erreur chargement préférences')
  return res.json()
}

export async function updatePreferences(dto: PreferencesDto): Promise<PreferencesDto> {
  const res = await apiFetch(`${BASE}/me/preferences`, { method: 'PUT', body: JSON.stringify(dto) })
  if (!res.ok) throw new Error('Erreur enregistrement préférences')
  return res.json()
}

export async function getSecurite(): Promise<SecuriteDto> {
  const res = await apiFetch(`${BASE}/me/security`)
  if (!res.ok) throw new Error('Erreur chargement sécurité')
  return res.json()
}

export async function updateSecurite(dto: SecuriteDto): Promise<SecuriteDto> {
  const res = await apiFetch(`${BASE}/me/security`, { method: 'PUT', body: JSON.stringify(dto) })
  if (!res.ok) throw new Error('Erreur enregistrement sécurité')
  return res.json()
}

export async function getNotifications(): Promise<NotificationsDto> {
  const res = await apiFetch(`${BASE}/me/notifications`)
  if (!res.ok) throw new Error('Erreur chargement notifications')
  return res.json()
}

export async function updateNotifications(dto: NotificationsDto): Promise<NotificationsDto> {
  const res = await apiFetch(`${BASE}/me/notifications`, { method: 'PUT', body: JSON.stringify(dto) })
  if (!res.ok) throw new Error('Erreur enregistrement notifications')
  return res.json()
}
