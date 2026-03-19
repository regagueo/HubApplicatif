/**
 * Configuration des menus par rôle métier.
 * EMPLOYEE → Congés, Notes de frais, Chat, Paramètres
 * MANAGER → mêmes + Validation équipe
 * RH → mêmes + Ressources humaines + Indicateurs RH
 * ADMIN → Paramètres & Sécurité uniquement
 * DG → Tableau de bord global + Indicateurs consolidés
 */

import type { LucideIcon } from 'lucide-react'
import {
  LayoutDashboard,
  CalendarOff,
  Receipt,
  MessageCircle,
  Settings,
  Users,
  Building2,
  BarChart3,
  Shield
} from 'lucide-react'

export interface NavItem {
  path: string
  label: string
  icon: LucideIcon
  roles: string[]  // rôles autorisés (vide = tous)
}

export const ALL_NAV_ITEMS: NavItem[] = [
  { path: '/', label: 'Tableau de bord', icon: LayoutDashboard, roles: ['EMPLOYEE', 'MANAGER', 'RH'] },
  { path: '/chat', label: 'Chat collaboratif', icon: MessageCircle, roles: ['EMPLOYEE', 'MANAGER', 'RH'] },
  { path: '/conges', label: 'Congés & Absences', icon: CalendarOff, roles: ['EMPLOYEE', 'MANAGER', 'RH'] },
  { path: '/notes-frais', label: 'Notes de frais', icon: Receipt, roles: ['EMPLOYEE', 'MANAGER', 'RH'] },
  { path: '/validation-equipe', label: 'Validation équipe', icon: Users, roles: ['MANAGER', 'RH'] },
  { path: '/rh', label: 'Ressources humaines', icon: Building2, roles: ['RH'] },
  { path: '/indicateurs-internes', label: 'Indicateurs internes', icon: BarChart3, roles: ['EMPLOYEE', 'MANAGER', 'RH'] },
  { path: '/indicateurs-rh', label: 'Indicateurs RH', icon: BarChart3, roles: ['RH'] },
  { path: '/dashboard-dg', label: 'Tableau de bord global', icon: LayoutDashboard, roles: ['DG'] },
  { path: '/indicateurs-consolides', label: 'Indicateurs consolidés', icon: BarChart3, roles: ['DG'] },
  { path: '/settings', label: 'Paramètres', icon: Settings, roles: ['EMPLOYEE', 'MANAGER', 'RH', 'DG'] },
  { path: '/admin', label: 'Paramètres & Sécurité', icon: Shield, roles: ['ADMIN'] }
]

function normalizeRole(r: string): string {
  return r.startsWith('ROLE_') ? r.slice(5) : r
}

export function getNavItemsForRoles(userRoles: string[]): NavItem[] {
  if (!userRoles || userRoles.length === 0) return []
  const roles = userRoles.map(normalizeRole)

  // ADMIN : uniquement Paramètres & Sécurité
  if (roles.includes('ADMIN')) {
    return ALL_NAV_ITEMS.filter((item) => item.roles.includes('ADMIN'))
  }

  // DG : Tableau de bord global + Indicateurs consolidés
  if (roles.includes('DG')) {
    return ALL_NAV_ITEMS.filter((item) => item.roles.includes('DG'))
  }

  // RH, MANAGER, EMPLOYEE : menus cumulatifs
  const items: NavItem[] = []
  const seen = new Set<string>()

  for (const role of ['RH', 'MANAGER', 'EMPLOYEE'] as const) {
    if (!roles.includes(role)) continue
    for (const item of ALL_NAV_ITEMS) {
      if (item.roles.includes(role) && !seen.has(item.path)) {
        items.push(item)
        seen.add(item.path)
      }
    }
  }

  return items
}
