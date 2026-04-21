import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { issueService, commentService, userService } from '../services'
import { useAuth } from '../context/AuthContext'
import Topbar from '../components/layout/Topbar'
import Button from '../components/ui/Button'
import { Spinner, Skeleton } from '../components/ui'
import styles from './IssueDetailPage.module.css'

const STATUS_OPTS = [
  { value:'TODO', label:'To Do' }, { value:'IN_PROGRESS', label:'In Progress' },
  { value:'IN_REVIEW', label:'In Review' }, { value:'DONE', label:'Done' },
]

export default function IssueDetailPage() {
  const { issueId } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [issue,      setIssue]      = useState(null)
  const [comments,   setComments]   = useState([])
  const [allUsers,   setAllUsers]   = useState([])
  const [loading,    setLoading]    = useState(true)
  const [comment,    setComment]    = useState('')
  const [posting,    setPosting]    = useState(false)
  const [commentErr, setCommentErr] = useState('')
  const [updating,   setUpdating]   = useState(false)
  const [updatingAssignee, setUpdatingAssignee] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [iRes, cRes, uRes] = await Promise.all([
        issueService.getById(issueId),
        commentService.getAll(issueId),
        userService.getAll(),
      ])
      setIssue(iRes.data)
      setComments(cRes.data || [])
      setAllUsers(uRes.data || [])
    } catch { navigate(-1) }
    finally { setLoading(false) }
  }, [issueId, navigate])

  useEffect(() => { load() }, [load])

  const handleStatusChange = async (newStatus) => {
    setUpdating(true)
    try {
      const r = await issueService.updateStatus(issueId, newStatus)
      setIssue(r.data)
    } catch (err) { alert(err.response?.data?.message || 'Failed to update status.') }
    finally { setUpdating(false) }
  }

  const handleAssigneeChange = async (newAssigneeId) => {
    setUpdatingAssignee(true)
    try {
      const r = await issueService.update(issueId, {
        assigneeId: newAssigneeId ? Number(newAssigneeId) : null,
      })
      setIssue(r.data)
    } catch (err) { alert(err.response?.data?.message || 'Failed to update assignee.') }
    finally { setUpdatingAssignee(false) }
  }

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

  const handleDeleteIssue = async () => {
    if (!confirm('Delete this issue? This cannot be undone.')) return
    try { await issueService.delete(issueId); navigate(-1) }
    catch (err) { alert(err.response?.data?.message || 'Failed to delete issue.') }
  }

  if (loading) return (
    <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
      <Skeleton height={28} width="60%" /><Skeleton height={16} width="30%" />
    </div>
  )
  if (!issue) return null

  const assigneeOpts = [
    { value:'', label:'Unassigned' },
    ...allUsers.map(u => ({ value: String(u.id), label: `${u.name} (${u.email})` }))
  ]

  return (
    <div>
      <div className={styles.breadcrumb}>
        <button className={styles.back} onClick={() => navigate(-1)}>← Back</button>
        <span className={styles.breadSep}>/</span>
        <span style={{ fontFamily:'var(--font-mono)', color:'var(--c-cyan)', fontSize:13 }}>Issue #{issue.id}</span>
      </div>

      <div className={styles.layout}>
        {/* Main */}
        <main className={`${styles.main} afu`}>
          <div className={styles.titleSection}>
            <div className={styles.badgeRow}>
              <span className={`badge badge-${issue.type?.toLowerCase()}`}>{issue.type}</span>
              <span className={`badge badge-${issue.priority?.toLowerCase()}`}>{issue.priority}</span>
              <span className={`badge badge-${issue.status?.toLowerCase()}`}>{issue.status?.replace(/_/g,' ')}</span>
            </div>
            <h1 className={styles.title}>{issue.title}</h1>
          </div>

          {issue.description && (
            <div className={styles.descCard}>
              <h3 className={styles.cardLabel}>Description</h3>
              <p className={styles.desc}>{issue.description}</p>
            </div>
          )}

          {/* Comments */}
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
                    <div className={styles.commentAvatar}>{c.author?.name?.[0]?.toUpperCase() || '?'}</div>
                    <div className={styles.commentBody}>
                      <div className={styles.commentHeader}>
                        <span className={styles.commentAuthor}>{c.author?.name}</span>
                        {(c.author?.id === user?.id || user?.role === 'ADMIN') && (
                          <button className={styles.deleteComment} onClick={() => handleDeleteComment(c.id)} aria-label="Delete comment">
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
            <form onSubmit={handleComment} className={styles.commentForm} noValidate>
              <textarea className={styles.commentInput} placeholder="Leave a comment..."
                value={comment} onChange={e => setComment(e.target.value)} rows={3} aria-label="Comment text" />
              {commentErr && <p className={styles.commentErr} role="alert">{commentErr}</p>}
              <div style={{ display:'flex', justifyContent:'flex-end' }}>
                <Button type="submit" variant="primary" size="sm" loading={posting}>Post Comment</Button>
              </div>
            </form>
          </div>
        </main>

        {/* Sidebar */}
        <aside className={`${styles.sidebar} afu-2`}>
          {/* Status */}
          <div className={styles.sideCard}>
            <h3 className={styles.sideLabel}>Status</h3>
            <div style={{ position:'relative' }}>
              <select className={styles.sideSelect} value={issue.status}
                onChange={e => handleStatusChange(e.target.value)} disabled={updating} aria-label="Change status">
                {STATUS_OPTS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              {updating && <div style={{ position:'absolute', right:10, top:'50%', transform:'translateY(-50%)' }}><Spinner size={14} /></div>}
            </div>
          </div>

          {/* Assignee */}
          <div className={styles.sideCard}>
            <h3 className={styles.sideLabel}>Assignee</h3>
            <div style={{ position:'relative' }}>
              <select className={styles.sideSelect}
                value={issue.assignee ? String(issue.assignee.id) : ''}
                onChange={e => handleAssigneeChange(e.target.value || null)}
                disabled={updatingAssignee} aria-label="Change assignee">
                {assigneeOpts.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              {updatingAssignee && <div style={{ position:'absolute', right:10, top:'50%', transform:'translateY(-50%)' }}><Spinner size={14} /></div>}
            </div>
            {issue.assignee && (
              <div style={{ display:'flex', alignItems:'center', gap:8, marginTop:8 }}>
                <div style={{ width:24, height:24, background:'linear-gradient(135deg, var(--c-indigo), var(--c-blue))', borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', fontSize:11, fontWeight:700, color:'#fff', flexShrink:0 }}>
                  {issue.assignee.name[0].toUpperCase()}
                </div>
                <span style={{ fontSize:13, color:'var(--text-primary)' }}>{issue.assignee.name}</span>
              </div>
            )}
          </div>

          {/* Details */}
          <div className={styles.sideCard}>
            <h3 className={styles.sideLabel}>Details</h3>
            <div className={styles.detailRows}>
              <div className={styles.detailRow}>
                <span className={styles.detailKey}>Reporter</span>
                <span className={styles.detailVal}>{issue.reporter?.name}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailKey}>Sprint</span>
                <span className={styles.detailVal}>{issue.sprintId ? `Sprint #${issue.sprintId}` : <span style={{ color:'var(--text-muted)' }}>Backlog</span>}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailKey}>Story Points</span>
                <span style={{ fontFamily:'var(--font-mono)', color:'var(--c-cyan)', fontSize:13 }}>{issue.storyPoints || 0}</span>
              </div>
              <div className={styles.detailRow}>
                <span className={styles.detailKey}>Project</span>
                <button className={styles.projectLink} onClick={() => navigate(`/projects/${issue.projectId}`)}>
                  View Project →
                </button>
              </div>
            </div>
          </div>

          {/* Danger zone */}
          <div className={styles.sideCard}>
            <Button variant="danger" size="sm" fullWidth onClick={handleDeleteIssue}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/>
              </svg>
              Delete Issue
            </Button>
          </div>
        </aside>
      </div>
    </div>
  )
}