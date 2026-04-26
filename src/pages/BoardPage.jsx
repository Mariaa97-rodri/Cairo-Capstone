import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { sprintService, issueService } from '../services'
import Topbar from '../components/layout/Topbar'
import Button from '../components/ui/Button'
import { Spinner } from '../components/ui'
import styles from './BoardPage.module.css'

const COLUMNS = [
  { key: 'TODO',        label: 'To Do',      color: 'var(--status-todo)'  },
  { key: 'IN_PROGRESS', label: 'In Progress', color: 'var(--c-blue)'      },
  { key: 'IN_REVIEW',   label: 'In Review',   color: 'var(--c-cyan)'      },
  { key: 'DONE',        label: 'Done',        color: 'var(--status-done)' },
]

const PRIORITY_COLOR = {
  LOW: '#4ade80', MEDIUM: '#facc15', HIGH: '#fb923c', CRITICAL: '#f43f5e'
}

const STATUS_ORDER = ['TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE']

function IssueCard({ issue, onStatusChange, onClick }) {
  const [updating, setUpdating] = useState(false)
  const idx = STATUS_ORDER.indexOf(issue.status)

  const move = async (e, direction) => {
    e.stopPropagation()
    const newIdx = idx + direction
    if (newIdx < 0 || newIdx >= STATUS_ORDER.length) return
    setUpdating(true)
    try { await onStatusChange(issue.id, STATUS_ORDER[newIdx]) }
    finally { setUpdating(false) }
  }

  return (
    <div className={styles.issueCard} onClick={() => onClick(issue.id)}
      role="button" tabIndex={0}
      onKeyDown={e => e.key === 'Enter' && onClick(issue.id)}
      aria-label={issue.title}>

      <div className={styles.issueTop}>
        <span className={`badge badge-${issue.type?.toLowerCase()}`}>{issue.type}</span>
        <span style={{ width:8, height:8, borderRadius:'50%',
          background: PRIORITY_COLOR[issue.priority] || '#fff', flexShrink:0 }}
          title={`Priority: ${issue.priority}`} />
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

        {/* ← → navigation — stop click from opening issue detail */}
        <div style={{ display:'flex', gap:4 }} onClick={e => e.stopPropagation()}>
          {idx > 0 && (
            <button className={styles.moveBtn} style={{ opacity:0.65 }}
              onClick={e => move(e, -1)} disabled={updating}
              aria-label="Move back" title="Move back">
              {updating ? <Spinner size={11} /> : '←'}
            </button>
          )}
          {idx < STATUS_ORDER.length - 1 && (
            <button className={styles.moveBtn}
              onClick={e => move(e, 1)} disabled={updating}
              aria-label="Move forward" title="Move forward">
              {updating ? <Spinner size={11} /> : '→'}
            </button>
          )}
        </div>
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
        <div style={{ display:'flex', gap:8 }}>
          <Button variant="secondary" size="sm"
            onClick={() => navigate(`/projects/${id}/backlog`)}>
            + Add Issues from Backlog
          </Button>
          <Button variant="ghost" size="sm" onClick={load}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
              stroke="currentColor" strokeWidth="2">
              <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8"/>
              <path d="M21 3v5h-5M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16"/>
              <path d="M8 16H3v5"/>
            </svg>
            Refresh
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex-center" style={{ height:300 }}>
          <Spinner size={32} />
        </div>

      ) : error ? (
        <div className="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="1.2">
            <rect x="3" y="3" width="5" height="18" rx="1"/>
            <rect x="10" y="3" width="5" height="12" rx="1"/>
            <rect x="17" y="3" width="5" height="15" rx="1"/>
          </svg>
          <h3>No active sprint</h3>
          <p>{error}</p>
          <Button variant="primary" onClick={() => navigate(`/projects/${id}`)}>
            Go to Project
          </Button>
          <div style={{ marginTop:24, background:'var(--bg-card)', border:'1px solid var(--border)', borderRadius:'var(--radius-lg)', padding:'20px 24px', maxWidth:480, textAlign:'left' }}>
            <p style={{ fontSize:13, fontWeight:700, color:'var(--text-primary)', marginBottom:12 }}>
              How to get issues on the board:
            </p>
            {[
              'Go to your Project page',
              'Click "+ New Sprint" and create a sprint',
              'Go to Backlog and create issues',
              'In Backlog, click "+ Sprint" on each issue to assign it',
              'Back on the Project page, click "Start" on your sprint',
              'Come back here — your issues will appear!',
            ].map((step, i) => (
              <div key={i} style={{ display:'flex', gap:10, alignItems:'flex-start', marginBottom:8 }}>
                <span style={{ width:22, height:22, background:'var(--c-blue)', borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, fontWeight:700, color:'#fff', flexShrink:0 }}>
                  {i + 1}
                </span>
                <span style={{ fontSize:13, color:'var(--text-secondary)', lineHeight:1.5 }}>
                  {step}
                </span>
              </div>
            ))}
          </div>
        </div>

      ) : totalIssues === 0 ? (
        <div className="empty-state">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" strokeWidth="1.5">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
            <rect x="9" y="3" width="6" height="4" rx="1"/>
          </svg>
          <h3>Sprint is empty</h3>
          <p>No issues in this sprint yet. Go to Backlog and click "+ Sprint" on each issue.</p>
          <Button variant="primary" onClick={() => navigate(`/projects/${id}/backlog`)}>
            Go to Backlog
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
                      onClick={issueId => navigate(`/issues/${issueId}`)}
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