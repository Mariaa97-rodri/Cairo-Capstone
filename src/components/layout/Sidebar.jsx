import { NavLink, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import styles from './Sidebar.module.css'

const NAV = [
  { to: '/dashboard', label: 'Dashboard',
    icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg> },
  { to: '/projects',  label: 'Projects',
    icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M2 7h20M2 12h20M2 17h20"/><circle cx="5" cy="7" r="1" fill="currentColor"/><circle cx="5" cy="12" r="1" fill="currentColor"/><circle cx="5" cy="17" r="1" fill="currentColor"/></svg> },
]

const ADMIN_NAV = [
  { to: '/admin', label: 'Admin Panel',
    icon: <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg> },
]

export default function Sidebar() {
  const { user, logout, isAdmin } = useAuth()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const handleLogout = () => { logout(); navigate('/login') }

  const projectMatch = pathname.match(/^\/projects\/([^/]+)/)
  const projectId = projectMatch?.[1]

  return (
    <aside className={styles.sidebar} aria-label="Main navigation">
      {/* Logo */}
      <div className={styles.logo}>
        <div className={styles.logoMark}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
            <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5" stroke="var(--c-cyan)" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </div>
        <span className={styles.logoText}>Cairo</span>
      </div>

      <div className={styles.prismLine} />

      {/* Nav */}
      <nav className={styles.nav}>
        {NAV.map(({ to, label, icon }) => (
          <NavLink key={to} to={to}
            className={({ isActive }) => [styles.navItem, isActive ? styles.active : ''].filter(Boolean).join(' ')}>
            <span className={styles.navIcon}>{icon}</span>
            <span>{label}</span>
          </NavLink>
        ))}

        {/* Project section — visible when inside a project */}
        {projectId && (
          <>
            <div className={styles.navDivider} />
            <p className={styles.navSection}>Project</p>
            <NavLink to={`/projects/${projectId}/board`}
              className={({ isActive }) => [styles.navItem, isActive ? styles.active : ''].filter(Boolean).join(' ')}>
              <span className={styles.navIcon}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><rect x="3" y="3" width="5" height="18" rx="1"/><rect x="10" y="3" width="5" height="12" rx="1"/><rect x="17" y="3" width="5" height="15" rx="1"/></svg>
              </span>
              <span>Sprints</span>
            </NavLink>
            <NavLink to={`/projects/${projectId}/backlog`}
              className={({ isActive }) => [styles.navItem, isActive ? styles.active : ''].filter(Boolean).join(' ')}>
              <span className={styles.navIcon}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8"><path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/><rect x="9" y="3" width="6" height="4" rx="1"/><path d="M9 12h6M9 16h4"/></svg>
              </span>
              <span>Issues</span>
            </NavLink>
          </>
        )}

        {/* Admin section — only visible to ADMIN role */}
        {isAdmin && (
          <>
            <div className={styles.navDivider} />
            <p className={styles.navSection}>Admin</p>
            {ADMIN_NAV.map(({ to, label, icon }) => (
              <NavLink key={to} to={to}
                className={({ isActive }) => [styles.navItem, styles.adminItem, isActive ? styles.active : ''].filter(Boolean).join(' ')}>
                <span className={styles.navIcon}>{icon}</span>
                <span>{label}</span>
              </NavLink>
            ))}
          </>
        )}
      </nav>

      <div className={styles.spacer} />

      {/* User */}
      <div className={styles.user}>
        <div className={styles.avatar} aria-hidden="true">
          {user?.name?.[0]?.toUpperCase() || '?'}
        </div>
        <div className={styles.userInfo}>
          <p className={styles.userName}>{user?.name}</p>
          <p className={styles.userRole}>{user?.role}</p>
        </div>
        <button className={styles.logoutBtn} onClick={handleLogout} aria-label="Log out" title="Log out">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/>
          </svg>
        </button>
      </div>
    </aside>
  )
}