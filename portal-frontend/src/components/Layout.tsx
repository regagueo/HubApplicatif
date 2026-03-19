import { useState } from 'react'
import { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  LogOut,
  Menu,
  X
} from 'lucide-react'
import GlobalSearch from './GlobalSearch'
import { getNavItemsForRoles } from '../utils/roleNav'

interface LayoutProps {
  children: ReactNode
}

export default function Layout({ children }: LayoutProps) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const [sidebarOpen, setSidebarOpen] = useState(false)

  const navItems = getNavItemsForRoles(user?.roles ?? [])

  const navLinkStyle = (path: string) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    padding: '0.75rem 1rem',
    borderRadius: 'var(--radius-md)',
    color: location.pathname === path ? 'white' : 'rgba(255,255,255,0.8)',
    textDecoration: 'none',
    background: location.pathname === path ? 'rgba(255,255,255,0.15)' : 'transparent',
    transition: 'all var(--transition)'
  })

  // Si aucun menu (ex: rôle inconnu), rediriger vers login ou afficher message
  const hasNav = navItems.length > 0

  return (
    <div style={{ minHeight: '100vh', display: 'flex' }}>
      {/* Sidebar - fixe sur desktop */}
      <aside
        style={{
          width: '260px',
          minWidth: '260px',
          background: 'var(--bg-sidebar)',
          borderRight: '1px solid rgba(255,255,255,0.1)',
          display: 'flex',
          flexDirection: 'column',
          transition: 'transform 0.25s ease',
          overflow: 'hidden',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          zIndex: 100
        }}
        className={`layout-sidebar ${sidebarOpen ? 'open' : ''}`}
      >
        <div style={{
          padding: '1.25rem',
          borderBottom: '1px solid rgba(255,255,255,0.1)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between'
        }}>
          <Link to={hasNav ? navItems[0]?.path ?? '/' : '/'} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'white', textDecoration: 'none', fontWeight: 600 }}>
            <span>Portail Intranet</span>
          </Link>
          <button
            onClick={() => setSidebarOpen(false)}
            style={{
              display: 'none',
              background: 'none',
              border: 'none',
              color: 'white',
              cursor: 'pointer',
              padding: '0.25rem'
            }}
            className="sidebar-close"
          >
            <X size={20} />
          </button>
        </div>
        <nav style={{ flex: 1, padding: '1rem 0.75rem', display: 'flex', flexDirection: 'column', gap: '0.25rem' }}>
          {hasNav ? (
            navItems.map(({ path, label, icon: Icon }) => (
              <Link
                key={path}
                to={path}
                style={navLinkStyle(path)}
                onMouseEnter={(e) => {
                  if (location.pathname !== path) {
                    e.currentTarget.style.background = 'rgba(255,255,255,0.08)'
                    e.currentTarget.style.color = 'white'
                  }
                }}
                onMouseLeave={(e) => {
                  if (location.pathname !== path) {
                    e.currentTarget.style.background = 'transparent'
                    e.currentTarget.style.color = 'rgba(255,255,255,0.8)'
                  }
                }}
              >
                <Icon size={20} />
                {label}
              </Link>
            ))
          ) : (
            <div style={{ padding: '1rem', color: 'rgba(255,255,255,0.6)', fontSize: '0.875rem' }}>
              Aucun menu disponible pour votre profil.
            </div>
          )}
        </nav>
      </aside>

      {/* Overlay mobile */}
      {sidebarOpen && (
        <div
          onClick={() => setSidebarOpen(false)}
          style={{
            display: 'none',
            position: 'fixed',
            inset: 0,
            background: 'rgba(0,0,0,0.4)',
            zIndex: 99
          }}
          className="sidebar-overlay"
        />
      )}

      {/* Contenu principal */}
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          marginLeft: '260px',
          transition: 'margin-left 0.25s ease',
          minWidth: 0
        }}
        className="layout-main"
      >
        {/* Topbar */}
        <header style={{
          background: 'var(--bg-secondary)',
          borderBottom: '1px solid var(--border)',
          padding: '0.75rem 1.5rem',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '1rem',
          flexWrap: 'wrap'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', flex: 1, minWidth: 0 }}>
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              style={{
                display: 'flex',
                padding: '0.5rem',
                background: 'var(--bg-primary)',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius-md)',
                color: 'var(--text-primary)',
                cursor: 'pointer'
              }}
              className="sidebar-toggle"
            >
              <Menu size={20} />
            </button>
            {hasNav && (
              <div style={{ flex: 1, maxWidth: '400px' }}>
                <GlobalSearch />
              </div>
            )}
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <span style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
              Bienvenue, {user?.firstName && user?.lastName ? `${user.firstName} ${user.lastName}` : user?.username || 'Utilisateur'}
            </span>
            <button
              onClick={logout}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem',
                padding: '0.5rem 1rem',
                borderRadius: 'var(--radius-md)',
                border: '1px solid var(--border)',
                background: 'transparent',
                color: 'var(--text-primary)',
                cursor: 'pointer'
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = 'var(--bg-primary)'
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = 'transparent'
              }}
            >
              <LogOut size={18} />
              Déconnexion
            </button>
          </div>
        </header>

        <main style={{ flex: 1, overflow: 'auto', padding: '1.5rem' }}>
          {children}
        </main>
      </div>

      <style>{`
        @media (min-width: 1024px) {
          .layout-sidebar { width: 260px !important; min-width: 260px !important; transform: none !important; }
          .layout-main { margin-left: 260px !important; }
          .sidebar-close, .sidebar-overlay { display: none !important; }
          .sidebar-toggle { display: none !important; }
        }
        @media (max-width: 1023px) {
          .layout-sidebar { transform: translateX(-100%); }
          .layout-sidebar.open { transform: translateX(0) !important; }
          .layout-main { margin-left: 0 !important; }
        }
      `}</style>
    </div>
  )
}
