import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { userService } from '../services'
import { useAuth } from '../context/AuthContext'
import Topbar from '../components/layout/Topbar'
import Button from '../components/ui/Button'
import { Skeleton, Spinner } from '../components/ui'
import styles from './AdminPage.module.css'

export default function AdminPage() {
  const { user: currentUser } = useAuth()
  const navigate = useNavigate()
  const [users,   setUsers]   = useState([])
  const [loading, setLoading] = useState(true)
  const [updating, setUpdating] = useState(null) // userId being updated
  const [error,   setError]   = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const r = await userService.getAll()
      setUsers(r.data || [])
    } catch { setError('Failed to load users.') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => {
    // Redirect non-admins
    if (currentUser && currentUser.role !== 'ADMIN') {
      navigate('/dashboard')
      return
    }
    load()
  }, [currentUser, load, navigate])

  const handleRoleToggle = async (userId, currentRole) => {
    const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN'
    if (userId === currentUser.id) {
      alert("You can't change your own role.")
      return
    }
    setUpdating(userId)
    try {
      await userService.updateRole(userId, newRole)
      setUsers(prev => prev.map(u => u.id === userId ? { ...u, role: newRole } : u))
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update role.')
    } finally { setUpdating(null) }
  }

  return (
    <div>
      <Topbar title="Admin Panel" subtitle="Manage users and roles" />

      <div className={`${styles.notice} afu`}>
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
        </svg>
        <span>
          Promote users to <strong>ADMIN</strong> to grant them access to this panel and role management.
          Admins can also delete any comment in the system.
        </span>
      </div>

      {error && <p style={{ color: '#f43f5e', fontSize: 14, marginBottom: 16 }}>{error}</p>}

      {loading ? (
        <div className={styles.table}>
          {[1,2,3,4].map(i => (
            <div key={i} className={styles.row}>
              <Skeleton height={16} width="30%" />
              <Skeleton height={16} width="40%" />
              <Skeleton height={16} width="15%" />
            </div>
          ))}
        </div>
      ) : (
        <div className={`${styles.table} afu-1`}>
          <div className={`${styles.row} ${styles.headerRow}`}>
            <span>Name</span>
            <span>Email</span>
            <span>Role</span>
            <span>Action</span>
          </div>
          {users.map(u => (
            <div key={u.id} className={styles.row}>
              <span className={styles.nameCell}>
                <div className={styles.avatar}>{u.name[0].toUpperCase()}</div>
                <span>{u.name}</span>
                {u.id === currentUser.id && <span className={styles.youTag}>You</span>}
              </span>
              <span className={styles.email}>{u.email}</span>
              <span>
                <span className={`badge ${u.role === 'ADMIN' ? styles.adminBadge : styles.userBadge}`}>
                  {u.role}
                </span>
              </span>
              <span>
                {u.id !== currentUser.id ? (
                  <Button
                    variant={u.role === 'ADMIN' ? 'danger' : 'secondary'}
                    size="sm"
                    loading={updating === u.id}
                    onClick={() => handleRoleToggle(u.id, u.role)}
                  >
                    {u.role === 'ADMIN' ? 'Demote to User' : 'Promote to Admin'}
                  </Button>
                ) : (
                  <span style={{ fontSize: 12, color: 'var(--text-muted)' }}>—</span>
                )}
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}