import { apiFetch } from './client'

export type ConversationType = 'INDIVIDUAL' | 'GROUP' | 'ANNOUNCEMENT'

export interface MemberDto {
  userId: number
  username: string
  admin: boolean
}

export interface ConversationDto {
  id: number
  type: ConversationType
  name: string
  createdBy: number
  createdAt: string
  members: MemberDto[]
  unreadCount?: number
}

export interface MessageDto {
  id: number
  conversationId: number
  senderId: number
  senderUsername: string
  content: string
  createdAt: string
}

export interface CreateConversationRequest {
  type: ConversationType
  name: string
  memberUserIds?: number[]
  memberUsernames?: string[]
}

export async function fetchConversations(): Promise<ConversationDto[]> {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 10000)
  try {
    const res = await apiFetch('/chat/conversations', { signal: controller.signal })
    clearTimeout(timeout)
    if (!res.ok) throw new Error('Erreur lors du chargement des conversations')
    return res.json()
  } catch (e) {
    clearTimeout(timeout)
    if ((e as Error).name === 'AbortError') {
      throw new Error('Délai dépassé. Vérifiez que le chat-service (8085) est démarré.')
    }
    throw e
  }
}

export async function fetchConversation(id: number): Promise<ConversationDto> {
  const res = await apiFetch(`/chat/conversations/${id}`)
  if (!res.ok) throw new Error('Conversation introuvable')
  return res.json()
}

export async function createConversation(req: CreateConversationRequest): Promise<ConversationDto> {
  const res = await apiFetch('/chat/conversations', {
    method: 'POST',
    body: JSON.stringify(req)
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Erreur lors de la création')
  }
  return res.json()
}

export async function fetchMessages(conversationId: number, page = 0, size = 50): Promise<MessageDto[]> {
  const res = await apiFetch(`/chat/conversations/${conversationId}/messages?page=${page}&size=${size}`)
  if (!res.ok) throw new Error('Erreur lors du chargement des messages')
  const list = await res.json()
  return Array.isArray(list) ? list.reverse() : []
}

export async function sendMessage(conversationId: number, content: string): Promise<MessageDto> {
  const res = await apiFetch('/chat/messages', {
    method: 'POST',
    body: JSON.stringify({ conversationId, content })
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.message || 'Erreur lors de l\'envoi')
  }
  return res.json()
}
