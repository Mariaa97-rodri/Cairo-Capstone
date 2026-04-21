import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { sprintService, issueService } from '../services'
import { useAuth } from '../context/AuthContext'
import Topbar from '../components/layout/Topbar'
import Button from '../components/ui/Button'
import { Spinner } from '../components/ui'
import styles from './BoardPage.module.css'

const COLUMNS = [
  { key: 'TODO',        label: 'To Do',       color: 'var(--status-todo)' },
  { key: 'IN_PROGRESS', label: 'In Progress',  color: 'var(--c-blue)' },
  { key: 'IN_REVIEW',   label: 'In Review',    color: 'var(--c-cyan)' },
  { key: 'DONE',        label: 'Done',         color: 'var(--status-done)' },
]

const PRIORITY_COLOR = {
  LOW: '#4ade80', MEDIUM: '#facc15', HIGH: '#fb923c', CRITICAL: '#f43f5e'
}

function IssueCard({ issue, onStatusChange, onClick }) {
  const [updating, setUpdating] = useState(false)

  const moveNext = async (e) => {
    e.stopPropagation()
    const order = ['TODO','IN_PROGRESS','IN_REVIEW','DONE']
    const idx = order.indexOf(issue.status)
    if (idx >= order.length - 1) return
    setUpdating(true)
    try { await onStatusChange(issue.id, order[idx + 1]) }
    finally { setUpdating(false) }
  }

  return (
    <div
      className={styles.issueCard}
      onClick={() => onClick(issue.id)}
      role="button" tabIndex={0}
      onKeyDown={e => e.key === 'Enter' && onClick(issue.id)}
      aria-label={issue.title}
    >
      <div className={styles.issueTop}>
        <span className={`badge badge-${issue.type?.toLowerCase()}`}>{issue.type}</span>
        <span style={{ width: 8, height: 8, borderRadius: '50%', background: PRIORITY_COLOR[issue.priority] || '#fff', flexShrink: 0 }} aria-label={`Priority: ${issue.priority}`} />
      </div>
      <p className={styles.issueTitle}>{issue.title}</p>
      {issue.storyPoints > 0 && (
        <span className={styles.points}>{issue.storyPoints} pts</span>
      )}
      <div className={styles.issueBottom}>
        {issue.assignee ? (
          <span className={styles.assignee} title={issue.assignee.name}>
            {issue.assignee.name[0].toUpperCase()}
          </span>
        ) : (
          <span className={styles.unassigned}>Unassigned</span>
        )}
        {issue.status !== 'DONE' && (
          <button
            className={styles.moveBtn}
            onClick={moveNext}
            disabled={updating}
            aria-label="Move to next status"
            title="Move forward"
          >
            {updating ? <Spinner size={12} /> : '→'}
          </button>
        )}
      </div>
    </div>
  )
}

export default function BoardPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [board,   setBoard]   = useState({})
  const [loading, setLoading] = useState(true)
  const [error,   setError]   = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const r = await sprintService.getBoard(id)
      setBoard(r.data || {})
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load board.')
    } finally { setLoading(false) }
  }, [id])

  useEffect(() => { load() }, [load])

  const handleStatusChange = async (issueId, newStatus) => {
    await issueService.updateStatus(issueId, newStatus)
    load()
  }

  const totalIssues = Object.values(board).flat().length
  const doneCount   = (board['DONE'] || []).length

  return (
    <div className={styles.wrapper}>
      <Topbar
        title="Kanban Board"
        subtitle={totalIssues > 0 ? `${doneCount}/${totalIssues} issues done` : 'Active sprint'}
      />

      <div className={styles.headerRow}>
        <Button variant="ghost" size="sm" onClick={() => navigate(`/projects/${id}`)}>
          ← Back to Project
        </Button>
        <Button variant="secondary" size="sm" onClick={load}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
            <path d="M21 3v5h-5M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/>
            <path d="M8 16H3v5"/>
          </svg>
          Refresh
        </Button>
      </div>

      {loading ? (
        <div className="flex-center" style={{ height: 300 }}>
          <Spinner size={32} />
        </div>
      ) : error ? (
        <div className="empty-state">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
          </svg>
          <h3>No active sprint</h3>
          <p>{error}</p>
          <Button variant="primary" onClick={() => navigate(`/projects/${id}`)}>
            Go to Project
          </Button>
        </div>
      ) : (
        <div className={styles.board}>
          {COLUMNS.map(col => {
            const issues = board[col.key] || []
            return (
              <div key={col.key} className={styles.column}>
                <div className={styles.colHeader}>
                  <span className={styles.colDot} style={{ background: col.color }} />
                  <span className={styles.colLabel}>{col.label}</span>
                  <span className={styles.colCount}>{issues.length}</span>
                </div>
                <div className={styles.colLine} style={{ background: col.color }} />
                <div className={styles.colBody}>
                  {issues.length === 0 ? (
                    <div className={styles.empty}>No issues</div>
                  ) : issues.map(issue => (
                    <IssueCard
                      key={issue.id}
                      issue={issue}
                      onStatusChange={handleStatusChange}
                      onClick={(issueId) => navigate(`/issues/${issueId}`)}
                    />
                  ))}
                </div>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}