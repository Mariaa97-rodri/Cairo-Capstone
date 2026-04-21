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

  const [showModal, setShowModal] = useState(false)
  const [saving,    setSaving]    = useState(false)
  const [formErr,   setFormErr]   = useState('')
  const [formErrors,setFormErrors]= useState({})
  const [form, setForm] = useState({ title:'', description:'', type:'TASK', priority:'MEDIUM', storyPoints:'', sprintId:'', assigneeId:'' })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const params = {}
      if (filters.type)     params.type     = filters.type
      if (filters.priority) params.priority = filters.priority
      if (filters.status)   params.status   = filters.status

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
    if (!search.trim()) return issues
    const q = search.toLowerCase()
    return issues.filter(i => i.title.toLowerCase().includes(q))
  }, [issues, search])

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

  const sprintOpts  = [{ value:'', label:'Backlog (no sprint)' }, ...sprints.map(s => ({ value: String(s.id), label: `${s.name} (${s.status})` }))]
  const assigneeOpts= [{ value:'', label:'Unassigned' },          ...users.map(u => ({ value: String(u.id), label: `${u.name} (${u.email})` }))]

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
            <span>Title</span><span>Type</span><span>Priority</span><span>Status</span><span>Assignee</span><span>Pts</span>
          </div>
          {filtered.map(issue => (
            <div key={issue.id} className={styles.row}
              onClick={() => navigate(`/issues/${issue.id}`)}
              role="button" tabIndex={0}
              onKeyDown={e => e.key === 'Enter' && navigate(`/issues/${issue.id}`)}
              aria-label={`Open: ${issue.title}`}>
              <span className={styles.issueTitle}>{issue.title}</span>
              <span className={`badge badge-${issue.type?.toLowerCase()}`}>{issue.type}</span>
              <span className={`badge badge-${issue.priority?.toLowerCase()}`}>{issue.priority}</span>
              <span className={`badge badge-${issue.status?.toLowerCase()}`}>{issue.status?.replace(/_/g,' ')}</span>
              <span className={styles.assignee}>{issue.assignee?.name || <span style={{ color:'var(--text-muted)' }}>—</span>}</span>
              <span className={styles.pts}>{issue.storyPoints || '—'}</span>
            </div>
          ))}
        </div>
      )}

      {/* Create Issue Modal */}
      <Modal open={showModal} onClose={closeModal} title="Create Issue" width={560}>
        {formErr && <div style={{ background:'rgba(244,63,94,0.1)', border:'1px solid rgba(244,63,94,0.25)', borderRadius:'var(--radius-md)', padding:'10px 14px', fontSize:13, color:'#f43f5e' }}>{formErr}</div>}
        <form onSubmit={handleCreate} style={{ display:'flex', flexDirection:'column', gap:14 }} noValidate>
          <Input label="Title" required placeholder="What needs to be done?"
            value={form.title} onChange={e => setForm(f => ({ ...f, title: e.target.value }))} error={formErrors.title} />
          <Input label="Description" as="textarea" rows={3} placeholder="Add more details..."
            value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
            <Select label="Type *"     value={form.type}     onChange={e => setForm(f => ({ ...f, type:     e.target.value }))} options={C_TYPE_OPTS} />
            <Select label="Priority *" value={form.priority} onChange={e => setForm(f => ({ ...f, priority: e.target.value }))} options={C_PRI_OPTS} />
          </div>
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
            <Input label="Story Points" type="number" placeholder="0"
              value={form.storyPoints} onChange={e => setForm(f => ({ ...f, storyPoints: e.target.value }))} />
            <Select label="Sprint" value={form.sprintId} onChange={e => setForm(f => ({ ...f, sprintId: e.target.value }))} options={sprintOpts} />
          </div>
          {/* Assignee dropdown */}
          <Select label="Assignee" value={form.assigneeId} onChange={e => setForm(f => ({ ...f, assigneeId: e.target.value }))} options={assigneeOpts} />
          <div style={{ display:'flex', gap:10, justifyContent:'flex-end', paddingTop:4 }}>
            <Button variant="secondary" type="button" onClick={closeModal}>Cancel</Button>
            <Button variant="primary"   type="submit" loading={saving}>Create Issue</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}