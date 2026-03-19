import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'

// Appliquer le thème sauvegardé au chargement
try {
  const saved = localStorage.getItem('portail_settings')
  if (saved) {
    const { theme } = JSON.parse(saved)
    if (theme === 'dark') document.documentElement.setAttribute('data-theme', 'dark')
  }
} catch { /* ignore */ }

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </React.StrictMode>,
)
