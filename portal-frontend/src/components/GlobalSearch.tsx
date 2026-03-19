import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search as SearchIcon, User } from 'lucide-react'
import { fetchUsersForChat } from '../api/auth'

interface UserPeek {
  id: number
  username: string
}

export default function GlobalSearch() {
  const [query, setQuery] = useState('')
  const [users, setUsers] = useState<UserPeek[]>([])
  const [usersLoading, setUsersLoading] = useState(false)
  const [open, setOpen] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const navigate = useNavigate()

  useEffect(() => {
    if (!query || query.length < 2) {
      setUsers([])
      setOpen(false)
      return
    }
    setUsersLoading(true)
    setOpen(true)
    fetchUsersForChat()
      .then((list) => {
        const q = query.toLowerCase().trim()
        setUsers(list.filter((u) => u.username.toLowerCase().includes(q)))
      })
      .catch(() => setUsers([]))
      .finally(() => setUsersLoading(false))
  }, [query])

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false)
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleSelect = () => {
    navigate('/chat')
    setQuery('')
    setOpen(false)
  }

  return (
    <div ref={ref} style={{ position: 'relative', width: '100%', maxWidth: '320px' }}>
      <div style={{
        display: 'flex',
        alignItems: 'center',
        gap: '0.5rem',
        padding: '0.5rem 1rem',
        background: 'var(--bg-primary)',
        borderRadius: 'var(--radius-md)',
        border: '1px solid var(--border)'
      }}>
        <SearchIcon size={18} color="var(--text-secondary)" />
        <input
          type="search"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => query.length >= 2 && setOpen(true)}
          placeholder="Rechercher un utilisateur..."
          style={{
            flex: 1,
            border: 'none',
            background: 'transparent',
            color: 'var(--text-primary)',
            fontSize: '0.9rem',
            outline: 'none'
          }}
        />
      </div>
      {open && query.length >= 2 && (
        <div style={{
          position: 'absolute',
          top: '100%',
          left: 0,
          right: 0,
          marginTop: '4px',
          background: 'var(--bg-secondary)',
          borderRadius: 'var(--radius-md)',
          border: '1px solid var(--border)',
          boxShadow: '0 10px 25px rgba(0,0,0,0.1)',
          maxHeight: '320px',
          overflow: 'auto',
          zIndex: 1000
        }}>
          {usersLoading ? (
            <div style={{ padding: '1rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              Recherche...
            </div>
          ) : users.length === 0 ? (
            <div style={{ padding: '1rem', color: 'var(--text-secondary)', fontSize: '0.875rem' }}>
              Aucun utilisateur trouvé
            </div>
          ) : (
            users.map((u) => (
              <button
                key={u.id}
                type="button"
                onClick={() => handleSelect()}
                style={{
                  width: '100%',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '0.75rem',
                  padding: '0.75rem 1rem',
                  border: 'none',
                  background: 'transparent',
                  color: 'inherit',
                  cursor: 'pointer',
                  textAlign: 'left',
                  transition: 'background 0.15s'
                }}
                onMouseEnter={(e) => { e.currentTarget.style.background = 'var(--bg-primary)' }}
                onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
              >
                <div style={{
                  width: '36px',
                  height: '36px',
                  borderRadius: '8px',
                  background: 'rgba(59, 130, 246, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  color: 'var(--accent)'
                }}>
                  <User size={18} />
                </div>
                <div style={{ flex: 1, fontWeight: 600, fontSize: '0.9rem' }}>{u.username}</div>
              </button>
            ))
          )}
        </div>
      )}
    </div>
  )
}
