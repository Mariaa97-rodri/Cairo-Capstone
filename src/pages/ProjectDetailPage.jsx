import { useState, useEffect, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { projectService, sprintService, userService } from '../services'
import { useAuth } from '../context/AuthContext'
import Topbar from '../components/layout/Topbar'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Modal from '../components/ui/Modal'
import { Skeleton } from '../components/ui'
import styles from './ProjectDetailPage.module.css'

// Native date input that auto-opens the calendar picker on click
function DateInput({ label, value, onChange, min }) {
  const today = new Date().toISOString().split('T')[0]
  const minDate = min !== undefined ? min : today
  return (
    <div className="form-group">
      <label className="form-label">{label}</label>
      <input
        type="date"
        value={value}
        min={minDate}
        onChange={e => {
          if (e.target.value < minDate) return
          onChange(e)
        }}
        onClick={e => e.target.showPicker?.()}
        onFocus={e => { e.target.style.borderColor = 'var(--c-blue)'; e.target.showPicker?.() }}
        onBlur={e  => { e.target.style.borderColor = 'var(--border)' }}
        style={{
          width: '100%',
          background: 'var(--bg-input)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-md)',
          color: value ? 'var(--text-primary)' : 'var(--text-muted)',
          fontSize: 14,
          fontFamily: 'var(--font-display)',
          padding: '10px 14px',
          cursor: 'pointer',
          colorScheme: 'dark',
          transition: 'border-color 0.18s',
        }}
      />
    </div>
  )
}

export default function ProjectDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate  = useNavigate()

  const [project,   setProject]   = useState(null)
  const [sprints,   setSprints]   = useState([])
  const [allUsers,  setAllUsers]  = useState([])
  // memberIds tracks who is in this project — starts with owner, grows as members are added
  const [memberIds, setMemberIds] = useState(new Set())
  const [loading,   setLoading]   = useState(true)

  // Sprint create modal
  const [showSprint,   setShowSprint]   = useState(false)
  const [savingSprint, setSavingSprint] = useState(false)
  const [sprintForm,   setSprintForm]   = useState({ name: '', startDate: '', endDate: '' })
  const [sprintErr,    setSprintErr]    = useState('')

  // Sprint edit modal
  const [editSprint, setEditSprint] = useState(null)
  const [editForm,   setEditForm]   = useState({ name: '', startDate: '', endDate: '' })
  const [savingEdit, setSavingEdit] = useState(false)
  const [editErr,    setEditErr]    = useState('')

  // Project edit modal
  const [showEditProject, setShowEditProject] = useState(false)
  const [projectForm,     setProjectForm]     = useState({ name: '', description: '' })
  const [savingProject,   setSavingProject]   = useState(false)
  const [projectEditErr,  setProjectEditErr]  = useState('')

  // Member modal
  const [showMember,   setShowMember]   = useState(false)
  const [memberUserId, setMemberUserId] = useState('')
  const [savingMember, setSavingMember] = useState(false)
  const [memberErr,    setMemberErr]    = useState('')

const load = useCallback(async () => {
  setLoading(true)
  try {
    const [pRes, sRes, uRes, mRes] = await Promise.all([
      projectService.getById(id),
      sprintService.getAll(id),
      userService.getAll(),
      projectService.getMembers(id),
    ])
    setProject(pRes.data)
    setSprints(sRes.data || [])
    setAllUsers(uRes.data || [])
    const ids = new Set()
    ;(mRes.data || []).forEach(m => ids.add(m.id))
    setMemberIds(ids)
  } catch (err) {
    const status = err.response?.status
    if (status === 403 || status === 401) {
      // Removed from project — redirect to projects list
      alert('You no longer have access to this project.')
      navigate('/projects')
    } else {
      navigate('/projects')
    }
  }
  finally { setLoading(false) }
}, [id, navigate])
  // const load = useCallback(async () => {
  //   setLoading(true)
  //   try {
  //     const [pRes, sRes, uRes] = await Promise.all([
  //       projectService.getById(id),
  //       sprintService.getAll(id),
  //       userService.getAll(),
  //     ])
  //     const proj  = pRes.data
  //     const users = uRes.data || []
  //     setProject(proj)
  //     setSprints(sRes.data || [])
  //     setAllUsers(users)

  //     // Build memberIds — owner is always a member.
  //     // We load all issues to find everyone who has been assigned or reported,
  //     // then we also track anyone added via the Add Member button this session.
  //     // Since the backend has no GET /members endpoint, this is the best approach.
  //     const ids = new Set()
  //     ids.add(proj.owner.id)
  //     // Also add current user if they can see this project (they must be a member)
  //     if (user?.id) ids.add(user.id)
  //     setMemberIds(ids)
  //   } catch { navigate('/projects') }
  //   finally  { setLoading(false) }
  // }, [id, navigate, user?.id])

  useEffect(() => { load() }, [load])

  // ── Sprint: create ────────────────────────────────────────────
  const handleCreateSprint = async (e) => {
    e.preventDefault()
    if (!sprintForm.name.trim()) { setSprintErr('Sprint name is required'); return }
    setSavingSprint(true)
    try {
      await sprintService.create(id, sprintForm)
      setShowSprint(false)
      setSprintForm({ name: '', startDate: '', endDate: '' })
      setSprintErr('')
      load()
    } catch (err) { setSprintErr(err.response?.data?.message || 'Failed to create sprint.') }
    finally { setSavingSprint(false) }
  }

  // ── Sprint: edit ──────────────────────────────────────────────
  const openEditSprint = (sprint) => {
    setEditSprint(sprint)
    setEditForm({ name: sprint.name, startDate: sprint.startDate || '', endDate: sprint.endDate || '' })
    setEditErr('')
  }

  const handleEditSprint = async (e) => {
    e.preventDefault()
    if (!editForm.name.trim()) { setEditErr('Sprint name is required'); return }
    setSavingEdit(true)
    try {
      await sprintService.update(editSprint.id, editForm)
      setEditSprint(null)
      load()
    } catch (err) { setEditErr(err.response?.data?.message || 'Failed to update sprint.') }
    finally { setSavingEdit(false) }
  }

  // ── Sprint: lifecycle ─────────────────────────────────────────
  const handleStartSprint = async (sprintId) => {
    try { await sprintService.start(sprintId); load() }
    catch (err) { alert(err.response?.data?.message || 'Failed to start sprint.') }
  }

  const handleCompleteSprint = async (sprintId) => {
    if (!confirm('Complete this sprint? Unfinished issues return to backlog.')) return
    try { await sprintService.complete(sprintId); load() }
    catch (err) { alert(err.response?.data?.message || 'Failed to complete sprint.') }
  }

  // ── Project: edit ─────────────────────────────────────────────
  const openEditProject = () => {
    setProjectForm({ name: project.name, description: project.description || '' })
    setProjectEditErr('')
    setShowEditProject(true)
  }

  const handleEditProject = async (e) => {
    e.preventDefault()
    if (!projectForm.name.trim()) { setProjectEditErr('Project name is required'); return }
    setSavingProject(true)
    try {
      await projectService.update(id, projectForm)
      setShowEditProject(false)
      load()
    } catch (err) { setProjectEditErr(err.response?.data?.message || 'Failed to update project.') }
    finally { setSavingProject(false) }
  }

  // ── Members ───────────────────────────────────────────────────
  const handleAddMember = async (e) => {
    e.preventDefault()
    if (!memberUserId) { setMemberErr('Please select a user'); return }
    setSavingMember(true)
    try {
      await projectService.addMember(id, Number(memberUserId))
      // Immediately add to local memberIds so UI updates without a reload
      setMemberIds(prev => new Set([...prev, Number(memberUserId)]))
      setShowMember(false)
      setMemberUserId('')
      setMemberErr('')
    } catch (err) { setMemberErr(err.response?.data?.message || 'Failed to add member.') }
    finally { setSavingMember(false) }
  }

  const handleRemoveMember = async (userId) => {
    if (!confirm('Remove this member from the project?')) return
    try {
      await projectService.removeMember(id, userId)
      setMemberIds(prev => { const s = new Set(prev); s.delete(userId); return s })
    } catch (err) { alert(err.response?.data?.message || 'Failed to remove member.') }
  }

  const handleDeleteProject = async () => {
    if (!confirm(`Delete project "${project.name}"? This cannot be undone.`)) return
    try { await projectService.delete(id); navigate('/projects') }
    catch (err) { alert(err.response?.data?.message || 'Failed to delete project.') }
  }

  const isOwner  = project?.owner?.id === user?.id
  const isAdmin  = user?.role === 'ADMIN'
  const canManage = isOwner || isAdmin

  // Members = all users whose IDs are in memberIds
  const members    = allUsers.filter(u => memberIds.has(u.id))
  const nonMembers = allUsers.filter(u => !memberIds.has(u.id))

  if (loading) return (
    <div style={{ display:'flex', flexDirection:'column', gap:12 }}>
      <Skeleton height={28} width="40%" /><Skeleton height={16} width="25%" />
    </div>
  )
  if (!project) return null

  return (
    <div>
      <Topbar title={project.name} subtitle={`Key: ${project.projectKey}`} />

      {/* Action bar */}
      <div className={`${styles.actions} afu`}>
        <Button variant="primary" onClick={() => navigate(`/projects/${id}/board`)}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="3" width="5" height="18" rx="1"/>
            <rect x="10" y="3" width="5" height="12" rx="1"/>
            <rect x="17" y="3" width="5" height="15" rx="1"/>
          </svg>
          Kanban Board
        </Button>
        <Button variant="secondary" onClick={() => navigate(`/projects/${id}/backlog`)}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
            <rect x="9" y="3" width="6" height="4" rx="1"/>
          </svg>
          Backlog
        </Button>
        {canManage && (
          <>
            <Button variant="ghost" onClick={() => setShowSprint(true)}>+ New Sprint</Button>
            <Button variant="ghost" onClick={() => setShowMember(true)}>+ Add Member</Button>
            <Button variant="ghost" onClick={openEditProject}>✏️ Edit Project</Button>
          </>
        )}
      </div>

      <div className={styles.layout}>
        {/* Left: Sprints */}
        <section className={`${styles.section} afu-2`}>
          <h2 className={styles.sectionTitle}>
            Sprints <span className={styles.count}>{sprints.length}</span>
          </h2>

          {/* How to use the board — always visible tip */}
          <div className={styles.tip}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
            </svg>
            <span>
              <strong>How it works:</strong> Create a sprint → Go to <strong>Backlog</strong> and assign issues to it → Start the sprint → Click <strong>View Board</strong> to see your Kanban board.
            </span>
          </div>

          {sprints.length === 0 ? (
            <div className="empty-state" style={{ padding:'32px 16px' }}>
              <h3>No sprints yet</h3>
              <p>Create a sprint to start organising work into time-boxed cycles</p>
              {canManage && <Button variant="primary" size="sm" onClick={() => setShowSprint(true)}>Create Sprint</Button>}
            </div>
          ) : (
            <div className={styles.sprintList}>
              {sprints.map(s => (
                <div key={s.id} className={styles.sprintCard}>
                  <div className={styles.sprintTop}>
                    <div style={{ display:'flex', flexDirection:'column', gap:6 }}>
                      <span className={`badge badge-${s.status.toLowerCase()}`}>{s.status.replace('_',' ')}</span>
                      <h3 className={styles.sprintName}>{s.name}</h3>
                      {(s.startDate || s.endDate) && (
                        <p className={styles.sprintDates}>📅 {s.startDate || '—'} → {s.endDate || '—'}</p>
                      )}
                    </div>
                    <div className={styles.sprintBtns}>
                      {s.status === 'ACTIVE' && (
                        <>
                          <Button size="sm" variant="primary" onClick={() => navigate(`/projects/${id}/board`)}>
                            View Board
                          </Button>
                          {canManage && (
                            <Button size="sm" variant="secondary" onClick={() => handleCompleteSprint(s.id)}>
                              Complete
                            </Button>
                          )}
                        </>
                      )}
                      {s.status === 'PENDING' && (
                        <>
                          {canManage && (
                            <>
                              <Button size="sm" variant="cyan" onClick={() => handleStartSprint(s.id)}>
                                Start
                              </Button>
                              <Button size="sm" variant="ghost" onClick={() => openEditSprint(s)}>
                                ✏️ Edit
                              </Button>
                            </>
                          )}
                        </>
                      )}
                      {s.status === 'COMPLETED' && (
                        <span className={styles.completedTag}>✓ Completed</span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Right: Info + Members */}
        <aside className={`${styles.aside} afu-3`}>
          {/* Project Info */}
          <div className={styles.infoCard}>
            <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:12 }}>
              <h3 className={styles.infoTitle}>Project Info</h3>
              {canManage && (
                <button className={styles.addMemberBtn} onClick={openEditProject}>✏️ Edit</button>
              )}
            </div>
            <div className={styles.infoRow}>
              <span className={styles.infoLabel}>Key</span>
              <span style={{ fontFamily:'var(--font-mono)', color:'var(--c-cyan)', fontSize:13 }}>{project.projectKey}</span>
            </div>
            <div className={styles.infoRow}>
              <span className={styles.infoLabel}>Owner</span>
              <span className={styles.infoValue}>{project.owner?.name}</span>
            </div>
            {project.description && (
              <div className={styles.infoRow} style={{ flexDirection:'column', gap:4 }}>
                <span className={styles.infoLabel}>Description</span>
                <span style={{ fontSize:13, color:'var(--text-secondary)', lineHeight:1.5 }}>{project.description}</span>
              </div>
            )}
          </div>

          {/* Team Members */}
          <div className={styles.infoCard}>
            <div style={{ display:'flex', alignItems:'center', justifyContent:'space-between', marginBottom:12 }}>
              <h3 className={styles.infoTitle}>
                Team Members
                <span className={styles.count} style={{ marginLeft:6 }}>{members.length}</span>
              </h3>
              {canManage && (
                <button className={styles.addMemberBtn} onClick={() => setShowMember(true)}>+ Add</button>
              )}
            </div>

            {members.length === 0 ? (
              <p style={{ fontSize:12, color:'var(--text-muted)' }}>No members yet.</p>
            ) : (
              <div style={{ display:'flex', flexDirection:'column', gap:8 }}>
                {members.map(m => (
                  <div key={m.id} className={styles.memberRow}>
                    <div className={styles.memberAvatar}>{m.name?.[0]?.toUpperCase()}</div>
                    <div className={styles.memberInfo}>
                      <span className={styles.memberName}>{m.name}</span>
                      <span className={styles.memberRole}>
                        {m.id === project.owner.id ? 'Owner' : m.role}
                      </span>
                    </div>
                    {canManage && m.id !== project.owner.id && (
                      <button
                        className={styles.removeMemberBtn}
                        onClick={() => handleRemoveMember(m.id)}
                        title={`Remove ${m.name}`}
                        aria-label={`Remove ${m.name}`}
                      >
                        ✕
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Danger zone */}
          {canManage && (
            <div className={styles.infoCard}>
              <h3 className={styles.infoTitle} style={{ color:'#f43f5e' }}>Danger Zone</h3>
              <Button variant="danger" size="sm" fullWidth onClick={handleDeleteProject}>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M3 6h18M8 6V4h8v2M19 6l-1 14H6L5 6"/>
                </svg>
                Delete Project
              </Button>
            </div>
          )}
        </aside>
      </div>

      {/* ── Create Sprint Modal ── */}
      <Modal open={showSprint} onClose={() => { setShowSprint(false); setSprintErr('') }} title="New Sprint">
        {sprintErr && <div style={{ background:'rgba(244,63,94,0.1)', border:'1px solid rgba(244,63,94,0.25)', borderRadius:'var(--radius-md)', padding:'10px 14px', fontSize:13, color:'#f43f5e' }}>{sprintErr}</div>}
        <form onSubmit={handleCreateSprint} style={{ display:'flex', flexDirection:'column', gap:14 }} noValidate>
          <Input label="Sprint Name" required placeholder="e.g. Sprint 1"
            value={sprintForm.name} onChange={e => setSprintForm(f => ({ ...f, name: e.target.value }))} />
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
            <DateInput label="Start Date" value={sprintForm.startDate}
              onChange={e => setSprintForm(f => ({ ...f, startDate: e.target.value }))} />
            <DateInput label="End Date" value={sprintForm.endDate}
              min={sprintForm.startDate || undefined}
              onChange={e => setSprintForm(f => ({ ...f, endDate: e.target.value }))} />
          </div>
          <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
            <Button variant="secondary" type="button" onClick={() => setShowSprint(false)}>Cancel</Button>
            <Button variant="cyan" type="submit" loading={savingSprint}>Create Sprint</Button>
          </div>
        </form>
      </Modal>

      {/* ── Edit Sprint Modal ── */}
      <Modal open={!!editSprint} onClose={() => { setEditSprint(null); setEditErr('') }} title={`Edit ${editSprint?.name || 'Sprint'}`}>
        {editErr && <div style={{ background:'rgba(244,63,94,0.1)', border:'1px solid rgba(244,63,94,0.25)', borderRadius:'var(--radius-md)', padding:'10px 14px', fontSize:13, color:'#f43f5e' }}>{editErr}</div>}
        <form onSubmit={handleEditSprint} style={{ display:'flex', flexDirection:'column', gap:14 }} noValidate>
          <Input label="Sprint Name" required placeholder="e.g. Sprint 1"
            value={editForm.name} onChange={e => setEditForm(f => ({ ...f, name: e.target.value }))} />
          <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:12 }}>
            <DateInput label="Start Date" value={editForm.startDate}
              onChange={e => setEditForm(f => ({ ...f, startDate: e.target.value }))} />
            <DateInput label="End Date" value={editForm.endDate}
              min={editForm.startDate || undefined}
              onChange={e => setEditForm(f => ({ ...f, endDate: e.target.value }))} />
          </div>
          <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
            <Button variant="secondary" type="button" onClick={() => setEditSprint(null)}>Cancel</Button>
            <Button variant="cyan" type="submit" loading={savingEdit}>Save Changes</Button>
          </div>
        </form>
      </Modal>

      {/* ── Edit Project Modal ── */}
      <Modal open={showEditProject} onClose={() => setShowEditProject(false)} title="Edit Project">
        {projectEditErr && <div style={{ background:'rgba(244,63,94,0.1)', border:'1px solid rgba(244,63,94,0.25)', borderRadius:'var(--radius-md)', padding:'10px 14px', fontSize:13, color:'#f43f5e' }}>{projectEditErr}</div>}
        <form onSubmit={handleEditProject} style={{ display:'flex', flexDirection:'column', gap:14 }} noValidate>
          <Input label="Project Name" required placeholder="Project name"
            value={projectForm.name} onChange={e => setProjectForm(f => ({ ...f, name: e.target.value }))} />
          <Input label="Description" as="textarea" rows={3} placeholder="Project description"
            value={projectForm.description} onChange={e => setProjectForm(f => ({ ...f, description: e.target.value }))} />
          <p style={{ fontSize:12, color:'var(--text-muted)' }}>Note: Project Key cannot be changed after creation.</p>
          <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
            <Button variant="secondary" type="button" onClick={() => setShowEditProject(false)}>Cancel</Button>
            <Button variant="primary" type="submit" loading={savingProject}>Save Changes</Button>
          </div>
        </form>
      </Modal>

      {/* ── Add Member Modal ── */}
      <Modal open={showMember} onClose={() => { setShowMember(false); setMemberErr(''); setMemberUserId('') }} title="Add Team Member">
        {memberErr && <div style={{ background:'rgba(244,63,94,0.1)', border:'1px solid rgba(244,63,94,0.25)', borderRadius:'var(--radius-md)', padding:'10px 14px', fontSize:13, color:'#f43f5e' }}>{memberErr}</div>}
        <p style={{ fontSize:13, color:'var(--text-secondary)' }}>
          Select a registered user to add to this project. Once added they will appear in the Team Members list and can be assigned to issues.
        </p>
        {nonMembers.length === 0 ? (
          <div style={{ textAlign:'center', padding:'24px 0', color:'var(--text-muted)', fontSize:13 }}>
            All registered users are already members of this project.
          </div>
        ) : (
          <form onSubmit={handleAddMember} style={{ display:'flex', flexDirection:'column', gap:14 }} noValidate>
            <div style={{ display:'flex', flexDirection:'column', gap:8, maxHeight:280, overflowY:'auto' }}>
              {nonMembers.map(u => (
                <div key={u.id} onClick={() => setMemberUserId(String(u.id))}
                  style={{
                    display:'flex', alignItems:'center', gap:12,
                    padding:'10px 14px',
                    background: memberUserId === String(u.id) ? 'rgba(95,128,249,0.12)' : 'var(--bg-input)',
                    border: `1px solid ${memberUserId === String(u.id) ? 'var(--c-blue)' : 'var(--border)'}`,
                    borderRadius:'var(--radius-md)',
                    cursor:'pointer', transition:'all 0.15s',
                  }}>
                  <div style={{ width:34, height:34, background:'linear-gradient(135deg, var(--c-indigo), var(--c-blue))', borderRadius:'50%', display:'flex', alignItems:'center', justifyContent:'center', fontSize:13, fontWeight:700, color:'#fff', flexShrink:0 }}>
                    {u.name[0].toUpperCase()}
                  </div>
                  <div>
                    <div style={{ fontSize:14, fontWeight:600, color:'var(--text-primary)' }}>{u.name}</div>
                    <div style={{ fontSize:12, color:'var(--text-muted)' }}>{u.email} · {u.role}</div>
                  </div>
                  {memberUserId === String(u.id) && (
                    <div style={{ marginLeft:'auto', color:'var(--c-cyan)', fontSize:16, fontWeight:700 }}>✓</div>
                  )}
                </div>
              ))}
            </div>
            <div style={{ display:'flex', gap:10, justifyContent:'flex-end' }}>
              <Button variant="secondary" type="button" onClick={() => setShowMember(false)}>Cancel</Button>
              <Button variant="primary" type="submit" loading={savingMember} disabled={!memberUserId}>Add Member</Button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  )
}