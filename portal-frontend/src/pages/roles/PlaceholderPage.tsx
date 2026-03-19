import { Construction } from 'lucide-react'

interface PlaceholderPageProps {
  title: string
  description?: string
}

export default function PlaceholderPage({ title, description }: PlaceholderPageProps) {
  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '300px',
      gap: '1rem',
      color: 'var(--text-secondary)'
    }}>
      <Construction size={48} />
      <h2 style={{ fontSize: '1.25rem', fontWeight: 600, color: 'var(--text-primary)' }}>{title}</h2>
      <p style={{ textAlign: 'center', maxWidth: '400px' }}>
        {description || 'Cet écran sera prochainement disponible.'}
      </p>
    </div>
  )
}
