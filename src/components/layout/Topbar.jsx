import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { notificationService } from '../../services'
import styles from './Topbar.module.css'

export default function Topbar({ title, subtitle }) {
  const [count,  setCount]  = useState(0)
  const [open,   setOpen]   = useState(false)
  const [notifs, setNotifs] = useState([])
  const ref = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {
    notificationService.getCount()
      .then(r => setCount(r.data.unread))
      .catch(() => {})
  }, [])

  useEffect(() => {
    const handleClick = (e) => { if (ref.current && !ref.current.contains(e.target)) setOpen(false) }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  const openPanel = async () => {
    if (!open) {
      try {
        const r = await notificationService.getUnread()
        setNotifs(r.data)
      } catch {}
    }
    setOpen(o => !o)
  }

  const markAllRead = async () => {
    try {
      await notificationService.markAllRead()
      setCount(0)
      setNotifs(prev => prev.map(n => ({ ...n, isRead: true })))
    } catch {}
  }

  return (
    <header className={styles.topbar}>
      <div>
        {title    && <h1 className={styles.title}>{title}</h1>}
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      </div>

      <div className={styles.actions}>
        {/* Notification bell */}
        <div className={styles.bellWrap} ref={ref}>
          <button
            className={styles.bell}
            onClick={openPanel}
            aria-label={`Notifications${count > 0 ? `, ${count} unread` : ''}`}
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0"/>
            </svg>
            {count > 0 && (
              <span className={styles.badge} aria-hidden="true">{count > 9 ? '9+' : count}</span>
            )}
          </button>

          {open && (
            <div className={`${styles.panel} asci`} role="region" aria-label="Notifications">
              <div className={styles.panelHeader}>
                <span>Notifications</span>
                {count > 0 && (
                  <button className={styles.markRead} onClick={markAllRead}>Mark all read</button>
                )}
              </div>
              <div className={styles.panelBody}>
                {notifs.length === 0 ? (
                  <p className={styles.empty}>You're all caught up!</p>
                ) : notifs.map(n => (
                  <div key={n.id} className={[styles.notif, n.isRead ? styles.read : ''].filter(Boolean).join(' ')}>
                    <span className={styles.notifDot} aria-hidden="true" />
                    <div>
                      <p className={styles.notifMsg}>{n.message}</p>
                      <p className={styles.notifType}>{n.type.replace(/_/g, ' ')}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>
    </header>
  )
}