import { Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/layout/ProtectedRoute'
import AppLayout from './components/layout/AppLayout'
import LoginPage         from './pages/LoginPage'
import RegisterPage      from './pages/RegisterPage'
import DashboardPage     from './pages/DashboardPage'
import ProjectsPage      from './pages/ProjectsPage'
import ProjectDetailPage from './pages/ProjectDetailPage'
import BoardPage         from './pages/BoardPage'
import BacklogPage       from './pages/BacklogPage'
import IssueDetailPage   from './pages/IssueDetailPage'
import AdminPage         from './pages/AdminPage'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        {/* Public */}
        <Route path="/login"    element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Protected — all wrapped in sidebar layout */}
        <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
          <Route path="/dashboard"                element={<DashboardPage />} />
          <Route path="/projects"                 element={<ProjectsPage />} />
          <Route path="/projects/:id"             element={<ProjectDetailPage />} />
          <Route path="/projects/:id/board"       element={<BoardPage />} />
          <Route path="/projects/:id/backlog"     element={<BacklogPage />} />
          <Route path="/issues/:issueId"          element={<IssueDetailPage />} />
          <Route path="/admin"                    element={<AdminPage />} />
        </Route>

        <Route path="/"  element={<Navigate to="/dashboard" replace />} />
        <Route path="*"  element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AuthProvider>
  )
}