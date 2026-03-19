import { useState, useEffect, useRef } from 'react'
import { useAuth } from '../context/AuthContext'
import {
  MessageCircle,
  Send,
  Users,
  Megaphone,
  User,
  Plus,
  Paperclip,
  Loader2,
  Search
} from 'lucide-react'
import {
  fetchConversations,
  fetchMessages,
  sendMessage as apiSendMessage,
  createConversation,
  type ConversationDto,
  type MessageDto,
  type ConversationType
} from '../api/chat'
import { fetchUsersForChat, type UserPeek } from '../api/auth'
import { format } from 'date-fns'
import './Chat.css'

const CONV_TYPE_ICON: Record<ConversationType, React.ReactNode> = {
  INDIVIDUAL: <User size={18} />,
  GROUP: <Users size={18} />,
  ANNOUNCEMENT: <Megaphone size={18} />
}

export default function Chat() {
  const { user } = useAuth()
  const [conversations, setConversations] = useState<ConversationDto[]>([])
  const [conversationsLoading, setConversationsLoading] = useState(true)
  const [conversationsError, setConversationsError] = useState<string | null>(null)
  const [selected, setSelected] = useState<ConversationDto | null>(null)
  const [messages, setMessages] = useState<MessageDto[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [showNewConv, setShowNewConv] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)

  const loadConversations = async () => {
    setConversationsLoading(true)
    setConversationsError(null)
    try {
      const list = await fetchConversations()
      setConversations(list)
    } catch (e) {
      setConversationsError((e as Error).message)
    } finally {
      setConversationsLoading(false)
    }
  }

  useEffect(() => {
    loadConversations()
  }, [])

  useEffect(() => {
    if (!selected) {
      setMessages([])
      return
    }
    let cancelled = false
    setLoading(true)
    setError(null)
    fetchMessages(selected.id)
      .then((list) => { if (!cancelled) setMessages(list) })
      .catch((e) => { if (!cancelled) setError((e as Error).message) })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [selected?.id])

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  const send = async () => {
    const content = input.trim()
    if (!content || !selected) return
    setSending(true)
    setInput('')
    try {
      const msg = await apiSendMessage(selected.id, content)
      setMessages((prev) => {
        if (prev.some((m) => m.id === msg.id)) return prev
        return [...prev, msg]
      })
    } catch (e) {
      setError((e as Error).message)
      setInput(content)
    } finally {
      setSending(false)
    }
  }

  const canSend = selected && (
    selected.type !== 'ANNOUNCEMENT'
    || selected.members.some((m) => m.userId === user?.id && m.admin)
  )

  return (
    <div className="chat-page">
      <aside className="chat-sidebar">
        <div className="chat-sidebar-header">
          <h2>
            <MessageCircle size={24} />
            Messages
          </h2>
          <button
            type="button"
            className="btn-icon"
            onClick={() => setShowNewConv(true)}
            title="Nouvelle conversation"
          >
            <Plus size={20} />
          </button>
        </div>
        {conversationsLoading ? (
          <div className="chat-loading">
            <Loader2 size={24} className="spin" />
          </div>
        ) : conversationsError ? (
          <div className="chat-error">
            {conversationsError}
            <button type="button" onClick={loadConversations} style={{ marginTop: '0.5rem', padding: '0.5rem 1rem' }}>
              Réessayer
            </button>
          </div>
        ) : (
          <ul className="chat-conv-list">
            {conversations.map((c) => (
              <li key={c.id}>
                <button
                  type="button"
                  className={`chat-conv-item ${selected?.id === c.id ? 'active' : ''}`}
                  onClick={() => setSelected(c)}
                >
                  <span className="chat-conv-icon">{CONV_TYPE_ICON[c.type]}</span>
                  <span className="chat-conv-name">{c.name}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
      </aside>

      <main className="chat-main">
        {!selected ? (
          <div className="chat-empty">
            <MessageCircle size={48} />
            <p>Sélectionnez une conversation ou créez-en une nouvelle.</p>
          </div>
        ) : (
          <>
            <header className="chat-header">
              <span className="chat-header-icon">{CONV_TYPE_ICON[selected.type]}</span>
              <h3>{selected.name}</h3>
            </header>

            <div className="chat-messages">
              {loading ? (
                <div className="chat-loading">
                  <Loader2 size={24} className="spin" />
                </div>
              ) : error ? (
                <div className="chat-error">{error}</div>
              ) : (
                messages.map((m) => {
                  const isMe = m.senderId === user?.id
                  return (
                    <div
                      key={m.id}
                      className={`chat-bubble ${isMe ? 'me' : 'other'}`}
                    >
                      <div className="chat-bubble-avatar">
                        {m.senderUsername.charAt(0).toUpperCase()}
                      </div>
                      <div className="chat-bubble-content">
                        <div className="chat-bubble-meta">
                          <span className="chat-bubble-sender">{m.senderUsername}</span>
                          <span className="chat-bubble-time">
                            {format(new Date(m.createdAt), 'HH:mm')}
                          </span>
                        </div>
                        <div className="chat-bubble-text">{m.content}</div>
                      </div>
                    </div>
                  )
                })
              )}
              <div ref={messagesEndRef} />
            </div>

            {canSend && (
              <footer className="chat-footer">
                <button type="button" className="btn-icon" title="Pièce jointe">
                  <Paperclip size={20} />
                </button>
                <input
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && send()}
                  placeholder="Écrire un message..."
                  disabled={sending}
                />
                <button
                  type="button"
                  className="btn-send"
                  onClick={send}
                  disabled={sending || !input.trim()}
                >
                  {sending ? <Loader2 size={18} className="spin" /> : <Send size={18} />}
                  Envoyer
                </button>
              </footer>
            )}
          </>
        )}
      </main>

      {showNewConv && (
        <NewConversationModal
          onClose={() => setShowNewConv(false)}
          onCreated={(c) => {
            setConversations((prev) =>
              prev.some((x) => x.id === c.id) ? prev : [...prev, c]
            )
            setSelected(c)
            setShowNewConv(false)
          }}
        />
      )}
    </div>
  )
}

function NewConversationModal({
  onClose,
  onCreated
}: {
  onClose: () => void
  onCreated: (c: ConversationDto) => void
}) {
  const { user } = useAuth()
  const [type, setType] = useState<ConversationType>('INDIVIDUAL')
  const [name, setName] = useState('')
  const [users, setUsers] = useState<UserPeek[]>([])
  const [usersLoading, setUsersLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [selectedUser, setSelectedUser] = useState<UserPeek | null>(null)
  const [selectedMemberIds, setSelectedMemberIds] = useState<Set<number>>(new Set())
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setUsersLoading(true)
    fetchUsersForChat()
      .then((list) => setUsers(list.filter((u) => u.id !== user?.id)))
      .catch(() => setUsers([]))
      .finally(() => setUsersLoading(false))
  }, [user?.id])

  const resetSelection = () => {
    setSelectedUser(null)
    setSelectedMemberIds(new Set())
    setSearch('')
    setError(null)
  }

  useEffect(() => {
    if (type === 'ANNOUNCEMENT' && !user?.roles?.includes('ADMIN')) {
      setType('INDIVIDUAL')
      resetSelection()
    }
  }, [type, user?.roles])

  useEffect(() => {
    if (type === 'INDIVIDUAL') {
      setName(selectedUser ? `Discussion avec ${selectedUser.username}` : '')
    } else {
      setName('')
    }
  }, [type, selectedUser])

  const filteredUsers = users.filter((u) =>
    u.username.toLowerCase().includes(search.toLowerCase().trim())
  )

  const allUsersSelected =
    users.length > 0 && users.every((u) => selectedMemberIds.has(u.id))

  const selectAllUsers = () => {
    if (allUsersSelected) {
      setSelectedMemberIds(new Set())
    } else {
      setSelectedMemberIds(new Set(users.map((u) => u.id)))
    }
  }

  const toggleMember = (id: number) => {
    setSelectedMemberIds((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })
  }

  const submit = async () => {
    if (type === 'ANNOUNCEMENT' && !user?.roles?.includes('ADMIN')) {
      setError('Seuls les administrateurs peuvent créer un canal d\'annonces')
      return
    }
    if (type === 'INDIVIDUAL') {
      if (!selectedUser) {
        setError('Sélectionnez un contact')
        return
      }
      if (!name.trim()) setName(`Discussion avec ${selectedUser.username}`)
    } else {
      if (!name.trim()) {
        setError('Le nom du groupe ou canal est requis')
        return
      }
      if (selectedMemberIds.size === 0) {
        setError('Sélectionnez au moins un membre')
        return
      }
    }
    setLoading(true)
    setError(null)
    try {
      const memberUserIds =
        type === 'INDIVIDUAL'
          ? [selectedUser!.id]
          : Array.from(selectedMemberIds)
      const memberUsernames =
        type === 'INDIVIDUAL'
          ? [selectedUser!.username]
          : users.filter((u) => selectedMemberIds.has(u.id)).map((u) => u.username)
      const c = await createConversation({
        type,
        name: name.trim() || `Discussion avec ${selectedUser!.username}`,
        memberUserIds,
        memberUsernames
      })
      onCreated(c)
    } catch (e) {
      setError((e as Error).message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content chat-modal chat-modal-wide" onClick={(e) => e.stopPropagation()}>
        <h3>Nouvelle conversation</h3>
        <div className="form-group">
          <label>Type</label>
          <select
            value={type}
            onChange={(e) => {
              setType(e.target.value as ConversationType)
              resetSelection()
            }}
          >
            <option value="INDIVIDUAL">Privée</option>
            <option value="GROUP">Groupe</option>
            {user?.roles?.includes('ADMIN') && (
              <option value="ANNOUNCEMENT">Annonces</option>
            )}
          </select>
        </div>

        {type !== 'INDIVIDUAL' && (
          <div className="form-group">
            <label>Nom du {type === 'GROUP' ? 'groupe' : 'canal'}</label>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={type === 'GROUP' ? 'Ex: Équipe projet' : 'Ex: Annonces générales'}
            />
          </div>
        )}

        <div className="form-group">
          <label>
            {type === 'INDIVIDUAL' ? 'Choisir un contact' : 'Sélectionner les membres'}
          </label>
          <div className="chat-user-search">
            <Search size={18} />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Rechercher un utilisateur..."
            />
          </div>
          {(type === 'GROUP' || type === 'ANNOUNCEMENT') && (
            <button
              type="button"
              className="chat-select-all-btn"
              onClick={selectAllUsers}
              disabled={usersLoading || users.length === 0}
            >
              {allUsersSelected ? 'Tout désélectionner' : 'Sélectionner tous'}
            </button>
          )}
          <div className="chat-user-list">
            {usersLoading ? (
              <div className="chat-loading">
                <Loader2 size={24} className="spin" />
              </div>
            ) : filteredUsers.length === 0 ? (
              <p className="chat-user-empty">Aucun utilisateur trouvé</p>
            ) : (
              filteredUsers.map((u) => {
                if (type === 'INDIVIDUAL') {
                  const isSelected = selectedUser?.id === u.id
                  return (
                    <button
                      key={u.id}
                      type="button"
                      className={`chat-user-item ${isSelected ? 'selected' : ''}`}
                      onClick={() => setSelectedUser(isSelected ? null : u)}
                    >
                      <span className="chat-user-avatar">{u.username.charAt(0).toUpperCase()}</span>
                      <span className="chat-user-name">{u.username}</span>
                    </button>
                  )
                }
                const isChecked = selectedMemberIds.has(u.id)
                return (
                  <label key={u.id} className={`chat-user-item ${isChecked ? 'selected' : ''}`}>
                    <input
                      type="checkbox"
                      checked={isChecked}
                      onChange={() => toggleMember(u.id)}
                    />
                    <span className="chat-user-avatar">{u.username.charAt(0).toUpperCase()}</span>
                    <span className="chat-user-name">{u.username}</span>
                  </label>
                )
              })
            )}
          </div>
        </div>

        {error && <div className="chat-error">{error}</div>}
        <div className="modal-actions">
          <button type="button" className="btn-secondary" onClick={onClose}>
            Annuler
          </button>
          <button type="button" className="btn-primary" onClick={submit} disabled={loading}>
            {loading ? <Loader2 size={18} className="spin" /> : null}
            Créer
          </button>
        </div>
      </div>
    </div>
  )
}
