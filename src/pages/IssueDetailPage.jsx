import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { issueService, commentService, userService } from '../services'
import { useAuth } from '../context/AuthContext'
import Topbar from '../components/layout/Topbar'
import Button from '../components/ui/Button'
import { Spinner, Skeleton } from '../components/ui'
import styles from './IssueDetailPage.module.css'

const STATUS_OPTS = [
  { value:'TODO',        label:'To Do'       },
  { value:'IN_PROGRESS', label:'In Progress' },
  { value:'IN_REVIEW',   label:'In Review'   },
  { value:'DONE',        label:'Done'        },
]

// Maps status values to colors for the history timeline
const STATUS_COLOR = {
  TODO:        '#8892c8',
  IN_PROGRESS: '#5f80f9',
  IN_REVIEW:   '#19d9ef',
  DONE:        '#4ade80',
}

export default function IssueDetailPage() {
  const { issueId } = useParams()
  const { user }    = useAuth()
  const navigate    = useNavigate()

  const [issue,            setIssue]            = useState(null)
  const [comments,         setComments]         = useState([])
  const [history,          setHistory]          = useState([])
  const [allUsers,         setAllUsers]         = useState([])
  const [loading,          setLoading]          = useState(true)
  const [comment,          setComment]          = useState('')
  const [posting,          setPosting]          = useState(false)
  const [commentErr,       setCommentErr]       = useState('')
  const [updating,         setUpdating]         = useState(false)
  const [updatingAssignee, setUpdatingAssignee] = useState(false)

  const isAdmin      = user?.role === 'ADMIN'

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [iRes, cRes, uRes, hRes] = await Promise.all([
        issueService.getById(issueId),
        commentService.getAll(issueId),
        userService.getAll(),
        issueService.getHistory(issueId),
      ])
      setIssue(iRes.data)
      setComments(cRes.data  || [])
      setAllUsers(uRes.data  || [])
      setHistory(hRes.data   || [])
    } catch { navigate(-1) }
    finally  { setLoading(false) }
  }, [issueId, navigate])

  useEffect(() => { load() }, [load])

  // Permissions — computed after issue loads
  const isReporter        = issue?.reporter?.id === user?.id
  const canManageIssue    = isAdmin || isReporter   // edit / delete
  const canChangeAssignee = isAdmin || isReporter   // only reporter or admin

  // ── Status change — any authenticated user can move an issue ──
  const handleStatusChange = async (newStatus) => {
    setUpdating(true)
    try {
      const r = await issueService.updateStatus(issueId, newStatus)
      setIssue(r.data)
      // Refresh history so the new entry appears immediately
      const hRes = await issueService.getHistory(issueId)
      setHistory(hRes.data || [])
    } catch (err) { alert(err.response?.data?.message || 'Failed to update status.') }
    finally { setUpdating(false) }
  }

  // ── Assignee change — reporter or admin only ──────────────────
  const handleAssigneeChange = async (newAssigneeId) => {
    if (!canChangeAssignee) return
    setUpdatingAssignee(true)
    try {
      const r = await issueService.update(issueId, {
        assigneeId: newAssigneeId ? Number(newAssigneeId) : 0,
      })
      setIssue(r.data)
    } catch (err) { alert(err.response?.data?.message || 'Failed to update assignee.') }
    finally { setUpdatingAssignee(false) }
  }

  // ── Comments — any user can post, only author/admin can delete ─
  const handleComment = async (e) => {
    e.preventDefault()
    if (!comment.trim()) { setCommentErr('Comment cannot be empty'); return }
    setCommentErr('')
    setPosting(true)
    try {
      await commentService.add(issueId, comment)
      setComment('')
      const cRes = await commentService.getAll(issueId)
      setComments(cRes.data || [])
    } catch (err) { setCommentErr(err.response?.data?.message || 'Failed to post comment.') }
    finally { setPosting(false) }
  }

  const handleDeleteComment = async (commentId) => {
    if (!confirm('Delete this comment?')) return
    try {
      await commentService.delete(commentId)
      setComments(prev => prev.filter(c => c.id !== commentId))
    } catch (err) { alert(err.response?.data?.message || 'Failed to delete comment.') }
  }

  // ── Delete issue — admin only ─────────────────────────────────
  const handleDeleteIssue = async () => {
    if (!isAdmin) return
    if (!confirm('Delete this issue? This cannot be undone.')) return
    try {
      await issueService.delete(issueId)
      if (issue.sprintId) navigate(`/projects/${issue.projectId}/board`)
      else                navigate(`/projects/${issue.projectId}/backlog`)
    } catch (err) { alert(err.response?.data?.message || 'Failed to delete issue.') }
  }

  if (loading) return (
    <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
      <Skeleton height={28} width="60%" />
      <Skeleton height={16} width="30%" />
    </div>
  )
  if (!issue) return null

  const assigneeOpts = [
    { value:'', label:'Unassigned' },
    ...allUsers.map(u => ({ value: String(u.id), label: `${u.name} (${u.email})` }))
  ]

  return (
    <div>
      {/* Breadcrumb */}
      <div className={styles.breadcrumb}>
        <button className={styles.back} onClick={() => {
          if (issue.sprintId) navigate(`/projects/${issue.projectId}/board`)
          else                navigate(`/projects/${issue.projectId}/backlog`)
        }}>
          ← {issue.sprintId ? 'Back to Board' : 'Back to Backlog'}
        </button>
        <span className={styles.breadSep}>/</span>
        <span style={{ fontFamily:'var(--font-mono)', color:'var(--c-cyan)', fontSize:13 }}>
          Issue #{issue.id}
        </span>
      </div>

      <div className={styles.layout}>
        {/* ── Main column ── */}
        <main className={`${styles.main} afu`}>

          {/* Title + badges */}
          <div className={styles.titleSection}>
            <div className={styles.badgeRow}>
              <span className={`badge badge-${issue.type?.toLowerCase()}`}>{issue.type}</span>
              <span className={`badge badge-${issue.priority?.toLowerCase()}`}>{issue.priority}</span>
              <span className={`badge badge-${issue.status?.toLowerCase()}`}>{issue.status?.replace(/_/g,' ')}</span>
            </div>
            <h1 className={styles.title}>{issue.title}</h1>
          </div>

          {/* Description */}
          {issue.description && (
            <div className={styles.descCard}>
              <h3 className={styles.cardLabel}>Description</h3>
              <p className={styles.desc}>{issue.description}</p>
            </div>
          )}

          {/* ── Comments — visible to everyone ── */}
          <div className={styles.commentsSection}>
            <h3 className={styles.cardLabel}>
              Comments <span className={styles.commentCount}>{comments.length}</span>
            </h3>

            {comments.length === 0 ? (
              <p className={styles.noComments}>No comments yet. Be the first!</p>
            ) : (
              <div className={styles.commentList}>
                {comments.map(c => (
                  <div key={c.id} className={styles.comment}>
                    <div className={styles.commentAvatar}>
                      {c.author?.name?.[0]?.toUpperCase() || '?'}
                    </div>
                    <div className={styles.commentBody}>
                      <div className={styles.commentHeader}>
                        <span className={styles.commentAuthor}>{c.author?.name}</span>
                        {/* Delete button: author sees it for their own comments, admin sees all */}
                        {(c.author?.id === user?.id || isAdmin) && (
                          <button className={styles.deleteComment}
                            onClick={() => handleDeleteComment(c.id)}
                            aria-label="Delete comment"
                            title={isAdmin && c.author?.id !== user?.id ? 'Delete comment (Admin)' : 'Delete your comment'}>
                            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                              <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/>
                            </svg>
                          </button>
                        )}
                      </div>
                      <p className={styles.commentText}>{c.body}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}

            {/* Add comment form — available to everyone */}
            <form onSubmit={handleComment} className={styles.commentForm} noValidate>
              <textarea className={styles.commentInput}
                placeholder="Leave a comment..."
                value={comment}
                onChange={e => setComment(e.target.value)}
                rows={3} aria-label="Comment text" />
              {commentErr && <p className={styles.commentErr} role="alert">{commentErr}</p>}
              <div style={{ display:'flex', justifyContent:'flex-end' }}>
                <Button type="submit" variant="primary" size="sm" loading={posting}>
                  Post Comment
                </Button>
              </div>
            </form>
          </div>

          {/* ── Activity History — visible to everyone ── */}
          <div className={styles.commentsSection}>
            <h3 className={styles.cardLabel}>
              Activity History
              <span className={styles.commentCount}>{history.length}</span>
            </h3>

            {history.length === 0 ? (
              <p className={styles.noComments}>
                No history yet. Status changes will appear here.
              </p>
            ) : (
              <div style={{ display:'flex', flexDirection:'column' }}>
                {history.map((h, i) => {
                  const newColor = STATUS_COLOR[h.newValue] || 'var(--c-blue)'
                  const oldColor = STATUS_COLOR[h.oldValue] || 'var(--text-muted)'
                  return (
                    <div key={h.id} style={{
                      display:'flex', alignItems:'flex-start', gap:12,
                      padding:'10px 0',
                      borderBottom: i < history.length - 1 ? '1px solid var(--border)' : 'none',
                    }}>
                      {/* Timeline dot */}
                      <div style={{
                        width:8, height:8, borderRadius:'50%',
                        background: newColor,
                        flexShrink:0, marginTop:6,
                        boxShadow: `0 0 6px ${newColor}`,
                      }} />
                      <div style={{ flex:1 }}>
                        <p style={{ fontSize:13, color:'var(--text-primary)', lineHeight:1.5 }}>
                          <strong style={{ color:'var(--text-primary)' }}>{h.changedBy}</strong>
                          {' changed '}
                          <span style={{ color:'var(--c-cyan)', fontFamily:'var(--font-mono)', fontSize:12 }}>
                            {h.fieldName}
                          </span>
                          {h.oldValue && (
                            <>
                              {' from '}
                              <span style={{
                                background:'rgba(244,63,94,0.1)',
                                color: oldColor,
                                border: `1px solid ${oldColor}33`,
                                borderRadius:4, padding:'1px 6px',
                                fontSize:12, fontFamily:'var(--font-mono)',
                              }}>
                                {h.oldValue.replace(/_/g,' ')}
                              </span>
                            </>
                          )}
                          {' → '}
                          <span style={{
                            background:'rgba(74,222,128,0.08)',
                            color: newColor,
                            border: `1px solid ${newColor}33`,
                            borderRadius:4, padding:'1px 6px',
                            fontSize:12, fontFamily:'var(--font-mono)',
                          }}>
                            {h.newValue.replace(/_/g,' ')}
                          </span>
                        </p>
                        <p style={{ fontSize:11, color:'var(--text-muted)', marginTop:2, fontFamily:'var(--font-mono)' }}>
                          {new Date(h.changedAt).toLocaleString()}
                        </p>
                      </div>
                    </div>
                  )
                })}
              </div>
            )}
          </div>
        </main>

        {/* ── Sidebar ── */}
        <aside className={`${styles.sidebar} afu-2`}>

          {/* Status — any user can change */}
          <div className={styles.sideCard}>
            <h3 className={styles.sideLabel}>Status</h3>
            <div style={{ position:'relative' }}>
              <select className={styles.sideSelect} value={issue.status}
                onChange={e => handleStatusChange(e.target.value)}
                disabled={updating} aria-label="Change status">
                {STATUS_OPTS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              {updating && (
                <div style={{ position:'absolute', right:10, top:'50%', transform:'translateY(-50%)' }}>
                  <Spinner size={14} />
                </div>
              )}
            </div>
            <p style={{ fontSize:11, color:'var(--text-muted)', marginTop:4 }}>
              Anyone on the team can update the status.
            </p>
          </div>

          {/* Assignee — reporter or admin can change; others see read-only */}
          <div className={styles.sideCard}>
            <h3 className={styles.sideLabel}>
              Assignee
              {!canChangeAssignee && (
                <span style={{ fontSize:10, color:'var(--text-muted)', marginLeft:6, fontWeight:400, textTransform:'none', letterSpacing:0 }}>
                  (read only)
                </span>
              )}
            </h3>

            {canChangeAssignee ? (
              <>
                <div style={{ position:'relative' }}>
                  <select className={styles.sideSelect}
                    value={issue.assignee ? String(issue.assignee.id) : ''}
                    onChange={e => handleAssigneeChange(e.target.value || null)}
                    disabled={updatingAssignee} aria-label="Change assignee">
                    {assigneeOpts.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
                  </select>
                  {updatingAssignee && (
                    <div style={{ position:'absolute', right:10, top:'50%', transform:'translateY(-50%)' }}>
                      <Spinner size={14} />
                    </div>
                  )}
                </div>
                {issue.assignee && (
                  <div style={{ display:'flex', alignItems:'center', gap:8, marginTop:8 }}>
                    <div style={{ width:24, height:24, background:'linear-gradient(135deg, var(--c-indigo), var(--c-blue))', borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, fontWeight:700, color:'#fff' }}>
                      {issue.assignee.name[0].toUpperCase()}
                    </div>
                    <span style={{ fontSize:13, color:'var(--text-primary)' }}>
                      {issue.assignee.name}
                    </span>
                  </div>
                )}
              </>
            ) : (
              <div style={{ display:'flex', alignItems:'center', gap:8 }}>
                {issue.assignee ? (
                  <>
                    <div style={{ width:24, height:24, background:'linear-gradient(135deg, var(--c-indigo), var(--c-blue))', borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, fontWeight:700, color:'#fff' }}>
                      {issue.assignee.name[0].toUpperCase()}
                    </div>
                    <span style={{ fontSize:13, color:'var(--text-primary)' }}>
                      {issue.assignee.name}
                    </span>
                  </>
                ) : (
                  <span style={{ fontSize:13, color:'var(--text-muted)' }}>Unassigned</span>
                )}
              </div>
            )}
          </div>

          {/* Details — visible to everyone */}
          <div className={styles.sideCard}>
            <h3 className={styles.sideLabel}>Details</h3>
            <div className={styles.detailRows}>
              <div className={styles.detailRow}>
                <span className={styles.detailKey}>Reporter</span>
                <span className={styles.detailVal}>{issue.reporter?.name}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailKey}>Sprint</span>
                <span className={styles.detailVal}>
                  {issue.sprintId
                    ? <button className={styles.projectLink}
                        onClick={() => navigate(`/projects/${issue.projectId}/board`)}>
                        View Board →
                      </button>
                    : <span style={{ color:'var(--text-muted)' }}>Backlog</span>
                  }
                </span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailKey}>Story Points</span>
                <span style={{ fontFamily:'var(--font-mono)', color:'var(--c-cyan)', fontSize:13 }}>
                  {issue.storyPoints || 0}
                </span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailKey}>Project</span>
                <button className={styles.projectLink}
                  onClick={() => navigate(`/projects/${issue.projectId}`)}>
                  View Project →
                </button>
              </div>
            </div>
          </div>

          {/* Admin Actions — only visible to ADMIN */}
          {isAdmin && (
            <div className={styles.sideCard}>
              <h3 className={styles.sideLabel} style={{ color:'#f43f5e' }}>
                Admin Actions
              </h3>
              <p style={{ fontSize:12, color:'var(--text-muted)', marginBottom:10 }}>
                These actions are only available to admins.
              </p>
              <Button variant="danger" size="sm" fullWidth onClick={handleDeleteIssue}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/>
                </svg>
                Delete Issue
              </Button>
            </div>
          )}

          {/* Role info — helps users understand what they can/can't do */}
          <div className={styles.sideCard} style={{ background:'rgba(95,128,249,0.05)' }}>
            <h3 className={styles.sideLabel}>Your Permissions</h3>
            <div style={{ display:'flex', flexDirection:'column', gap:6, fontSize:12 }}>
              <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                <span style={{ color:'#4ade80' }}>✓</span>
                <span style={{ color:'var(--text-secondary)' }}>Update status</span>
              </div>
              <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                <span style={{ color:'#4ade80' }}>✓</span>
                <span style={{ color:'var(--text-secondary)' }}>Post comments</span>
              </div>
              <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                <span style={{ color: canChangeAssignee ? '#4ade80' : 'var(--text-muted)' }}>
                  {canChangeAssignee ? '✓' : '✗'}
                </span>
                <span style={{ color:'var(--text-secondary)' }}>
                  Change assignee {!canChangeAssignee && '(reporter or admin only)'}
                </span>
              </div>
              <div style={{ display:'flex', alignItems:'center', gap:6 }}>
                <span style={{ color: isAdmin ? '#4ade80' : 'var(--text-muted)' }}>
                  {isAdmin ? '✓' : '✗'}
                </span>
                <span style={{ color:'var(--text-secondary)' }}>
                  Delete issue {!isAdmin && '(admin only)'}
                </span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  )
}