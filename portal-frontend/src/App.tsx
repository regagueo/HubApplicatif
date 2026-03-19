import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Login from './pages/Login'
import EmployeeDashboard from './pages/roles/employee/Dashboard'
import Conges from './pages/roles/employee/Conges'
import NotesFrais from './pages/roles/employee/NotesFrais'
import Chat from './pages/Chat'
import Settings from './pages/Settings'
import PlaceholderPage from './pages/roles/PlaceholderPage'
import CongesRh from './pages/roles/rh/CongesRh'
import Layout from './components/Layout'

function normalizeRole(r: string): string {
  return r.startsWith('ROLE_') ? r.slice(5) : r
}

function hasRole(userRoles: string[] | undefined, role: string): boolean {
  if (!userRoles) return false
  return userRoles.some((r) => normalizeRole(r) === role)
}

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { token } = useAuth()
  if (!token) return <Navigate to="/login" replace />
  return <>{children}</>
}

function ProtectedLayout({ children }: { children: React.ReactNode }) {
  return <Layout>{children}</Layout>
}

function RoleRoute({ roles, children }: { roles: string[]; children: React.ReactNode }) {
  const { user } = useAuth()
  const userRoles = user?.roles ?? []
  const hasAccess = roles.some((r) => hasRole(userRoles, r))
  if (!hasAccess) return <Navigate to="/" replace />
  return <>{children}</>
}

function DefaultRedirect() {
  const { user } = useAuth()
  const roles = user?.roles ?? []

  if (hasRole(roles, 'ADMIN')) return <Navigate to="/admin" replace />
  if (hasRole(roles, 'DG')) return <Navigate to="/dashboard-dg" replace />
  return <EmployeeDashboard />
}

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <ProtectedLayout>
                <DefaultRedirect />
              </ProtectedLayout>
            </ProtectedRoute>
          }
        />
        <Route
          path="/conges"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['EMPLOYEE', 'MANAGER', 'RH']}>
                <ProtectedLayout>
                  <Conges />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/notes-frais"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['EMPLOYEE', 'MANAGER', 'RH']}>
                <ProtectedLayout>
                  <NotesFrais />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/chat"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['EMPLOYEE', 'MANAGER', 'RH']}>
                <ProtectedLayout>
                  <Chat />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/rh"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['RH']}>
                <ProtectedLayout>
                  <CongesRh />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/indicateurs-internes"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['EMPLOYEE', 'MANAGER', 'RH']}>
                <ProtectedLayout>
                  <PlaceholderPage
                    title="Indicateurs internes"
                    description="Moyennes générales et indicateurs internes. À venir."
                  />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/indicateurs-rh"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['RH']}>
                <ProtectedLayout>
                  <PlaceholderPage
                    title="Indicateurs RH"
                    description="Tableaux de bord et indicateurs Ressources humaines. À venir."
                  />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/dashboard-dg"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['DG']}>
                <ProtectedLayout>
                  <PlaceholderPage
                    title="Tableau de bord global"
                    description="Vue stratégique consolidée. À venir."
                  />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/indicateurs-consolides"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['DG']}>
                <ProtectedLayout>
                  <PlaceholderPage
                    title="Indicateurs consolidés"
                    description="Indicateurs globaux et validations finales. À venir."
                  />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/settings"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['EMPLOYEE', 'MANAGER', 'RH', 'DG']}>
                <ProtectedLayout>
                  <Settings />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
        <Route
          path="/admin"
          element={
            <ProtectedRoute>
              <RoleRoute roles={['ADMIN']}>
                <ProtectedLayout>
                  <Settings />
                </ProtectedLayout>
              </RoleRoute>
            </ProtectedRoute>
          }
        />
      </Routes>
    </AuthProvider>
  )
}

export default App
