import { useState, useEffect, useCallback, useMemo } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { issueService, sprintService, userService } from '../services'
import Topbar from '../components/layout/Topbar'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Modal from '../components/ui/Modal'
import { Select, Skeleton } from '../components/ui'
import styles from './BacklogPage.module.css'

const TYPE_OPTS     = [{ value:'',label:'All Types'},    {value:'BUG',label:'Bug'},{value:'STORY',label:'Story'},{value:'TASK',label:'Task'},{value:'EPIC',label:'Epic'}]
const PRIORITY_OPTS = [{ value:'',label:'All Priorities'},{value:'LOW',label:'Low'},{value:'MEDIUM',label:'Medium'},{value:'HIGH',label:'High'},{value:'CRITICAL',label:'Critical'}]
const STATUS_OPTS   = [{ value:'',label:'All Statuses'}, {value:'TODO',label:'To Do'},{value:'IN_PROGRESS',label:'In Progress'},{value:'IN_REVIEW',label:'In Review'},{value:'DONE',label:'Done'}]
const C_TYPE_OPTS   = TYPE_OPTS.slice(1)
const C_PRI_OPTS    = PRIORITY_OPTS.slice(1)

export default function BacklogPage() {
  const { id } = useParams()
  const navigate = useNavigate()

  const [issues,  setIssues]  = useState([])
  const [sprints, setSprints] = useState([])
  const [users,   setUsers]   = useState([])
  const [loading, setLoading] = useState(true)
  const [filters, setFilters] = useState({ type:'', priority:'', status:'' })
  const [search,  setSearch]  = useState('')

  // Create issue modal
  const [showModal,  setShowModal]  = useState(false)
  const [saving,     setSaving]     = useState(false)
  const [formErr,    setFormErr]    = useState('')
  const [formErrors, setFormErrors] = useState({})
  const [form, setForm] = useState({
    title:'', description:'', type:'TASK', priority:'MEDIUM',
    storyPoints:'', sprintId:'', assigneeId:''
  })

  // Add / reassign sprint modal
  const [addingIssue,    setAddingIssue]    = useState(null)
  const [addSprintId,    setAddSprintId]    = useState('')
  const [addingToSprint, setAddingToSprint] = useState(false)
  const [addSprintErr,   setAddSprintErr]   = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const params = {}
      if (filters.type)     params.type     = filters.type
      if (filters.priority) params.priority = filters.priority
      if (filters.status)   params.status   = filters.status
//default: hide DONE issues from backlog unless user explicitly filters by status
      if (!filters.status)  params.status   = undefined  // show all non-done issues
      const [iRes, sRes, uRes] = await Promise.all([
        issueService.getAll(id, params, 0, 100),
        sprintService.getAll(id),
        userService.getAll(),
      ])
      setIssues(iRes.data.content || [])
      setSprints(sRes.data || [])
      setUsers(uRes.data || [])
    } catch {}
    finally { setLoading(false) }
  }, [id, filters])

  useEffect(() => { load() }, [load])

 const filtered = useMemo(() => {
  let result = issues
  // Hide DONE issues by default — only show if user explicitly filters for DONE
  if (!filters.status) {
    result = result.filter(i => i.status !== 'DONE')
  }
  if (!search.trim()) return result
  const q = search.toLowerCase()
  return result.filter(i => i.title.toLowerCase().includes(q))
}, [issues, search, filters.status])

  // ── Create issue ──────────────────────────────────────────────
  const handleCreate = async (e) => {
    e.preventDefault()
    setFormErr('')
    const errs = {}
    if (!form.title.trim()) errs.title = 'Title is required'
    if (Object.keys(errs).length) { setFormErrors(errs); return }
    setFormErrors({})
    setSaving(true)
    try {
      await issueService.create(id, {
        title:       form.title,
        description: form.description || null,
        type:        form.type,
        priority:    form.priority,
        storyPoints: form.storyPoints ? Number(form.storyPoints) : 0,
        sprintId:    form.sprintId    ? Number(form.sprintId)    : null,
        assigneeId:  form.assigneeId  ? Number(form.assigneeId)  : null,
      })
      setShowModal(false)
      setForm({ title:'', description:'', type:'TASK', priority:'MEDIUM', storyPoints:'', sprintId:'', assigneeId:'' })
      load()
    } catch (err) { setFormErr(err.response?.data?.message || 'Failed to create issue.') }
    finally { setSaving(false) }
  }

  const closeModal = () => { setShowModal(false); setFormErrors({}); setFormErr('') }

  // ── Assign / reassign sprint ──────────────────────────────────
  const openSprintModal = (e, issue) => {
    e.stopPropagation()
    setAddingIssue(issue)
    setAddSprintId(issue.sprintId ? String(issue.sprintId) : '')
    setAddSprintErr('')
  }

const handleAddToSprint = async (e) => {
  e.preventDefault()
  setAddingToSprint(true)
  try {
    if (addSprintId) {
      // Assign or move to a sprint
      await sprintService.addIssue(Number(addSprintId), addingIssue.id)
    } else {
      // Remove from sprint — send sprintId=0 to tell backend to set null
      await issueService.update(addingIssue.id, { sprintId: 0 })
    }
    setAddingIssue(null)
    setAddSprintId('')
    setAddSprintErr('')
    load()
  } catch (err) {
    setAddSprintErr(err.response?.data?.message || 'Failed to update sprint. Try again.')
  } finally { setAddingToSprint(false) }
}

  // Only show non-completed sprints in the assign modal
  const activeSprintOpts = sprints.filter(s => s.status !== 'COMPLETED')
  const sprintOpts   = [{ value:'', label:'Backlog (no sprint)' }, ...sprints.map(s => ({ value: String(s.id), label: `${s.name} (${s.status})` }))]
  const assigneeOpts = [{ value:'', label:'Unassigned' }, ...users.map(u => ({ value: String(u.id), label: `${u.name} (${u.email})` }))]

  return (
    <div>
      <Topbar title="Backlog" subtitle={`${filtered.length} issue${filtered.length !== 1 ? 's' : ''}`} />

      <div className={`${styles.toolbar} afu`}>
        <input className={styles.search} placeholder="Search issues..." value={search}
          onChange={e => setSearch(e.target.value)} aria-label="Search issues" />
        <div className={styles.filters}>
          {[TYPE_OPTS, PRIORITY_OPTS, STATUS_OPTS].map((opts, i) => (
            <select key={i} className={styles.filter}
              value={[filters.type, filters.priority, filters.status][i]}
              onChange={e => setFilters(f => ({ ...f, [['type','priority','status'][i]]: e.target.value }))}
              aria-label={`Filter by ${['type','priority','status'][i]}`}>
              {opts.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
            </select>
          ))}
        </div>
        <div style={{ display:'flex', gap:8 }}>
          <Button variant="ghost" size="sm" onClick={() => navigate(`/projects/${id}`)}>← Project</Button>
          <Button variant="cyan"  size="sm" onClick={() => setShowModal(true)}>+ Issue</Button>
        </div>
      </div>

      {loading ? (
        <div className={styles.list}>
          {[1,2,3,4,5].map(i => (
            <div key={i} className={styles.row}>
              <Skeleton height={16} width="50%" />
              <Skeleton height={14} width="12%" />
              <Skeleton height={14} width="12%" />
            </div>
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <div className="empty-state afu">
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
            <rect x="9" y="3" width="6" height="4" rx="1"/>
          </svg>
          <h3>No issues found</h3>
          <p>Create your first issue or adjust your filters</p>
          <Button variant="primary" onClick={() => setShowModal(true)}>Create Issue</Button>
        </div>
      ) : (
        <div className={`${styles.list} afu-1`}>
          <div className={`${styles.row} ${styles.headerRow}`}>
            <span>Title</span>
            <span>Type</span>
            <span>Priority</span>
            <span>Status</span>
            <span>Assignee</span>
            <span>Pts</span>
            <span>Sprint</span>
          </div>

          {filtered.map(issue => {
            const sprintName = issue.sprintId
              ? (sprints.find(s => s.id === issue.sprintId)?.name || `Sprint #${issue.sprintId}`)
              : null

            return (
              <div key={issue.id} className={styles.row}
                onClick={() => navigate(`/issues/${issue.id}`)}
                role="button" tabIndex={0}
                onKeyDown={e => e.key === 'Enter' && navigate(`/issues/${issue.id}`)}
                aria-label={`Open: ${issue.title}`}>

                <span className={styles.issueTitle}>{issue.title}</span>
                <span className={`badge badge-${issue.type?.toLowerCase()}`}>{issue.type}</span>
                <span className={`badge badge-${issue.priority?.toLowerCase()}`}>{issue.priority}</span>
                <span className={`badge badge-${issue.status?.toLowerCase()}`}>{issue.status?.replace(/_/g,' ')}</span>
                <span className={styles.assignee}>
                  {issue.assignee?.name || <span style={{ color:'var(--text-muted)' }}>—</span>}
                </span>
                <span className={styles.pts}>{issue.storyPoints || '—'}</span>

                {/* Sprint column */}
                <span onClick={e => e.stopPropagation()}>
                  {sprintName ? (
                    <div style={{ display:'flex', alignItems:'center', gap:4 }}>
                      <span style={{ fontSize:11, color:'var(--c-cyan)', fontFamily:'var(--font-mono)', whiteSpace:'nowrap', overflow:'hidden', textOverflow:'ellipsis', maxWidth:90 }}>
                        {sprintName}
                      </span>
                      <button className={styles.addSprintBtn} style={{ fontSize:10, padding:'2px 6px' }}
                        onClick={e => openSprintModal(e, issue)} title="Change sprint">
                        ✎
                      </button>
                    </div>
                  ) : (
                    <button className={styles.addSprintBtn}
                      onClick={e => openSprintModal(e, issue)} title="Add to a sprint">
                      + Sprint
                    </button>
                  )}
                </span>
              </div>
            )
          })}
        </div>
      )}

      {/* ── Create Issue Modal ── */}
      <Modal open={showModal} onClose={closeModal} title="Create Issue" width={560}>
        {formErr && (
          <div style={{ background:'rgba(244,63,94,0.1)', border:'1px solid rgba(244,63,94,0.25)', borderRadius:'var(--radius-md)', padding:'10px 14px', fontSize:13, color:'#f43f5e' }}>
            {formErr}
          </div>
        )}
        <form onSubmit={handleCreate} style={{ display:'flex', flexDirection:'column', gap:14 }} noValidate>
          <Input label="Title" required placeholder="What needs to be done?"
            value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))}
            error={formErrors.title} />
          <Input label="Description" as="textarea" rows={3} placeholder="Add more details..."
            value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
            <Select label="Type *"     value={form.type}
              onChange={e => setForm(f => ({ ...f, type: e.target.value }))} options={C_TYPE_OPTS} />
            <Select label="Priority *" value={form.priority}
              onChange={e => setForm(f => ({ ...f, priority: e.target.value }))} options={C_PRI_OPTS} />
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
            <div className="form-group">
              <label className="form-label">Story Points</label>
              <input type="number" min="0" max="100" placeholder="0"
                value={form.storyPoints}
                onKeyDown={e => { if (['-','e','E','+','.'].includes(e.key)) e.preventDefault() }}
                onChange={e => { const v = e.target.value; if (v === '' || /^[0-9]+$/.test(v)) setForm(f => ({ ...f, storyPoints: v })) }}
                style={{ width:'100%', background:'var(--bg-input)', border:'1px solid var(--border)', borderRadius:'var(--radius-md)', color:'var(--text-primary)', fontSize:14, fontFamily:'var(--font-display)', padding:'10px 14px' }}
              />
            </div>
            <Select label="Sprint" value={form.sprintId}
              onChange={e => setForm(f => ({ ...f, sprintId: e.target.value }))} options={sprintOpts} />
          </div>
          <Select label="Assignee" value={form.assigneeId}
            onChange={e => setForm(f => ({ ...f, assigneeId: e.target.value }))} options={assigneeOpts} />
          <div style={{ display:'flex', gap:10, justifyContent:'flex-end', paddingTop:4 }}>
            <Button variant="secondary" type="button" onClick={closeModal}>Cancel</Button>
            <Button variant="primary"   type="submit" loading={saving}>Create Issue</Button>
          </div>
        </form>
      </Modal>

      {/* ── Assign / Reassign Sprint Modal ── */}
      <Modal
        open={!!addingIssue}
        onClose={() => { setAddingIssue(null); setAddSprintErr('') }}
        title={addingIssue?.sprintId ? 'Change Sprint' : 'Add to Sprint'}
      >
        {addSprintErr && (
          <div style={{ background:'rgba(244,63,94,0.1)', border:'1px solid rgba(244,63,94,0.25)', borderRadius:'var(--radius-md)', padding:'10px 14px', fontSize:13, color:'#f43f5e' }}>
            {addSprintErr}
          </div>
        )}
        <p style={{ fontSize:13, color:'var(--text-secondary)' }}>
          Issue: <strong style={{ color:'var(--text-primary)' }}>{addingIssue?.title}</strong>
        </p>

        {activeSprintOpts.length === 0 ? (
          <div style={{ textAlign:'center', padding:'20px 0', color:'var(--text-muted)', fontSize:13 }}>
            No active or pending sprints available. Create a sprint first.
          </div>
        ) : (
          <form onSubmit={handleAddToSprint} style={{ display:'flex', flexDirection:'column', gap:14 }} noValidate>
            <div style={{ display:'flex', flexDirection:'column', gap:8 }}>

              {/* Backlog option */}
              <div onClick={() => setAddSprintId('')}
                style={{
                  display:'flex', alignItems:'center', gap:12, padding:'10px 14px',
                  background: addSprintId === '' ? 'rgba(95,128,249,0.12)' : 'var(--bg-input)',
                  border: `1px solid ${addSprintId === '' ? 'var(--c-blue)' : 'var(--border)'}`,
                  borderRadius:'var(--radius-md)', cursor:'pointer', transition:'all 0.15s',
                }}>
                <div style={{ width:34, height:34, background:'var(--bg-hover)', border:'1px solid var(--border)', borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', fontSize:16, flexShrink:0 }}>
                  📋
                </div>
                <div>
                  <div style={{ fontSize:14, fontWeight:600, color:'var(--text-primary)' }}>Backlog</div>
                  <div style={{ fontSize:12, color:'var(--text-muted)' }}>Remove from sprint</div>
                </div>
                {addSprintId === '' && (
                  <div style={{ marginLeft:'auto', color:'var(--c-cyan)', fontSize:16, fontWeight:700 }}>✓</div>
                )}
              </div>

              {/* Sprint options */}
              {activeSprintOpts.map(s => (
                <div key={s.id} onClick={() => setAddSprintId(String(s.id))}
                  style={{
                    display:'flex', alignItems:'center', gap:12, padding:'10px 14px',
                    background: addSprintId === String(s.id) ? 'rgba(95,128,249,0.12)' : 'var(--bg-input)',
                    border: `1px solid ${addSprintId === String(s.id) ? 'var(--c-blue)' : 'var(--border)'}`,
                    borderRadius:'var(--radius-md)', cursor:'pointer', transition:'all 0.15s',
                  }}>
                  <div style={{ width:34, height:34, background:'linear-gradient(135deg, var(--c-indigo), var(--c-blue))', borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', fontSize:13, fontWeight:700, color:'#fff', flexShrink:0 }}>
                    {s.name[0].toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontSize:14, fontWeight:600, color:'var(--text-primary)' }}>{s.name}</div>
                    <div style={{ fontSize:12, color:'var(--text-muted)' }}>
                      {s.status}{s.startDate && s.endDate ? ` · ${s.startDate} → ${s.endDate}` : ''}
                    </div>
                  </div>
                  {addSprintId === String(s.id) && (
                    <div style={{ marginLeft:'auto', color:'var(--c-cyan)', fontSize:16, fontWeight:700 }}>✓</div>
                  )}
                </div>
              ))}
            </div>

            <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
              <Button variant="secondary" type="button" onClick={() => setAddingIssue(null)}>Cancel</Button>
              <Button variant="cyan" type="submit" loading={addingToSprint}>
                {addingIssue?.sprintId ? 'Move Issue' : 'Add to Sprint'}
              </Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  )
}