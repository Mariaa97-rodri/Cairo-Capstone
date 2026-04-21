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

export default function ProjectDetailPage() {
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()

  const [project,     setProject]     = useState(null)
  const [sprints,     setSprints]     = useState([])
  const [allUsers,    setAllUsers]    = useState([])
  const [loading,     setLoading]     = useState(true)

  // Sprint modal
  const [showSprint,  setShowSprint]  = useState(false)
  const [savingSprint,setSavingSprint]= useState(false)
  const [sprintForm,  setSprintForm]  = useState({ name: '', startDate: '', endDate: '' })
  const [sprintErr,   setSprintErr]   = useState('')

  // Member modal
  const [showMember,  setShowMember]  = useState(false)
  const [memberUserId,setMemberUserId]= useState('')
  const [savingMember,setSavingMember]= useState(false)
  const [memberErr,   setMemberErr]   = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [pRes, sRes, uRes] = await Promise.all([
        projectService.getById(id),
        sprintService.getAll(id),
        userService.getAll(),
      ])
      setProject(pRes.data)
      setSprints(sRes.data || [])
      setAllUsers(uRes.data || [])
    } catch { navigate('/projects') }
    finally  { setLoading(false) }
  }, [id, navigate])

  useEffect(() => { load() }, [load])

  // Sprint actions
  const handleStartSprint = async (sprintId) => {
    try { await sprintService.start(sprintId); load() }
    catch (err) { alert(err.response?.data?.message || 'Failed to start sprint.') }
  }

  const handleCompleteSprint = async (sprintId) => {
    if (!confirm('Complete this sprint? Unfinished issues return to backlog.')) return
    try { await sprintService.complete(sprintId); load() }
    catch (err) { alert(err.response?.data?.message || 'Failed to complete sprint.') }
  }

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

  // Member actions
  const handleAddMember = async (e) => {
    e.preventDefault()
    if (!memberUserId) { setMemberErr('Please select a user'); return }
    setSavingMember(true)
    try {
      await projectService.addMember(id, Number(memberUserId))
      setShowMember(false)
      setMemberUserId('')
      setMemberErr('')
      load()
    } catch (err) { setMemberErr(err.response?.data?.message || 'Failed to add member.') }
    finally { setSavingMember(false) }
  }

  const handleRemoveMember = async (userId) => {
    if (!confirm('Remove this member from the project?')) return
    try { await projectService.removeMember(id, userId); load() }
    catch (err) { alert(err.response?.data?.message || 'Failed to remove member.') }
  }

  const handleDeleteProject = async () => {
    if (!confirm(`Delete project "${project.name}"? This cannot be undone.`)) return
    try { await projectService.delete(id); navigate('/projects') }
    catch (err) { alert(err.response?.data?.message || 'Failed to delete project.') }
  }

  const isOwner = project?.owner?.id === user?.id

  // Users not already in this project (for the add member dropdown)
  // We don't have a members endpoint, so we show all users and let the backend reject duplicates
  const availableUsers = allUsers.filter(u => u.id !== project?.owner?.id || true)

  if (loading) return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <Skeleton height={28} width="40%" />
      <Skeleton height={16} width="25%" />
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
        {isOwner && (
          <>
            <Button variant="ghost" onClick={() => setShowSprint(true)}>+ New Sprint</Button>
            <Button variant="ghost" onClick={() => setShowMember(true)}>+ Add Member</Button>
          </>
        )}
      </div>

      <div className={styles.layout}>
        {/* Left: Sprints */}
        <section className={`${styles.section} afu-2`}>
          <h2 className={styles.sectionTitle}>
            Sprints
            <span className={styles.count}>{sprints.length}</span>
          </h2>
          {sprints.length === 0 ? (
            <div className="empty-state" style={{ padding: '32px 16px' }}>
              <h3>No sprints yet</h3>
              <p>Create a sprint to start organising work into time-boxed cycles</p>
              {isOwner && <Button variant="primary" size="sm" onClick={() => setShowSprint(true)}>Create Sprint</Button>}
            </div>
          ) : (
            <div className={styles.sprintList}>
              {sprints.map(s => (
                <div key={s.id} className={styles.sprintCard}>
                  <div className={styles.sprintTop}>
                    <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                      <span className={`badge badge-${s.status.toLowerCase()}`}>{s.status.replace('_',' ')}</span>
                      <h3 className={styles.sprintName}>{s.name}</h3>
                      {(s.startDate || s.endDate) && (
                        <p className={styles.sprintDates}>{s.startDate || '—'} → {s.endDate || '—'}</p>
                      )}
                    </div>
                    <div className={styles.sprintBtns}>
                      {s.status === 'ACTIVE' && (
                        <>
                          <Button size="sm" variant="primary" onClick={() => navigate(`/projects/${id}/board`)}>
                            View Board
                          </Button>
                          {isOwner && (
                            <Button size="sm" variant="secondary" onClick={() => handleCompleteSprint(s.id)}>
                              Complete
                            </Button>
                          )}
                        </>
                      )}
                      {s.status === 'PENDING' && isOwner && (
                        <Button size="sm" variant="cyan" onClick={() => handleStartSprint(s.id)}>
                          Start Sprint
                        </Button>
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
            <h3 className={styles.infoTitle}>Project Info</h3>
            <div className={styles.infoRow}>
              <span className={styles.infoLabel}>Key</span>
              <span style={{ fontFamily: 'var(--font-mono)', color: 'var(--c-cyan)', fontSize: 13 }}>{project.projectKey}</span>
            </div>
            <div className={styles.infoRow}>
              <span className={styles.infoLabel}>Owner</span>
              <span className={styles.infoValue}>{project.owner?.name}</span>
            </div>
            {project.description && (
              <div className={styles.infoRow} style={{ flexDirection: 'column', gap: 4 }}>
                <span className={styles.infoLabel}>Description</span>
                <span style={{ fontSize: 13, color: 'var(--text-secondary)', lineHeight: 1.5 }}>{project.description}</span>
              </div>
            )}
          </div>

          {/* Team Members */}
          <div className={styles.infoCard}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
              <h3 className={styles.infoTitle}>Team Members</h3>
              {isOwner && (
                <button className={styles.addMemberBtn} onClick={() => setShowMember(true)}>+ Add</button>
              )}
            </div>
            {/* Owner always shown */}
            <div className={styles.memberRow}>
              <div className={styles.memberAvatar}>{project.owner?.name?.[0]?.toUpperCase()}</div>
              <div className={styles.memberInfo}>
                <span className={styles.memberName}>{project.owner?.name}</span>
                <span className={styles.memberRole}>Owner</span>
              </div>
            </div>
            {/* All registered users shown as potential members — in a real app you'd have a members list endpoint */}
            <p style={{ fontSize: 12, color: 'var(--text-muted)', marginTop: 10 }}>
              Use "+ Add" to add team members by selecting from all registered users.
            </p>
          </div>

          {/* Danger zone — owner only */}
          {isOwner && (
            <div className={styles.infoCard}>
              <h3 className={styles.infoTitle} style={{ color: '#f43f5e' }}>Danger Zone</h3>
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

      {/* Create Sprint Modal */}
      <Modal open={showSprint} onClose={() => { setShowSprint(false); setSprintErr('') }} title="New Sprint">
        {sprintErr && <div style={{ background: 'rgba(244,63,94,0.1)', border: '1px solid rgba(244,63,94,0.25)', borderRadius: 'var(--radius-md)', padding: '10px 14px', fontSize: 13, color: '#f43f5e' }}>{sprintErr}</div>}
        <form onSubmit={handleCreateSprint} style={{ display: 'flex', flexDirection: 'column', gap: 14 }} noValidate>
          <Input label="Sprint Name" required placeholder="e.g. Sprint 1"
            value={sprintForm.name} onChange={e => setSprintForm(f => ({ ...f, name: e.target.value }))} />
          <Input label="Start Date" type="date"
            value={sprintForm.startDate} onChange={e => setSprintForm(f => ({ ...f, startDate: e.target.value }))} />
          <Input label="End Date" type="date"
            value={sprintForm.endDate} onChange={e => setSprintForm(f => ({ ...f, endDate: e.target.value }))} />
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
            <Button variant="secondary" type="button" onClick={() => setShowSprint(false)}>Cancel</Button>
            <Button variant="cyan" type="submit" loading={savingSprint}>Create Sprint</Button>
          </div>
        </form>
      </Modal>

      {/* Add Member Modal */}
      <Modal open={showMember} onClose={() => { setShowMember(false); setMemberErr(''); setMemberUserId('') }} title="Add Team Member">
        {memberErr && <div style={{ background: 'rgba(244,63,94,0.1)', border: '1px solid rgba(244,63,94,0.25)', borderRadius: 'var(--radius-md)', padding: '10px 14px', fontSize: 13, color: '#f43f5e' }}>{memberErr}</div>}
        <p style={{ fontSize: 13, color: 'var(--text-secondary)' }}>
          Select a registered user to add to this project. They will be able to create and work on issues.
        </p>
        <form onSubmit={handleAddMember} style={{ display: 'flex', flexDirection: 'column', gap: 14 }} noValidate>
          <div className="form-group">
            <label className="form-label">Select User *</label>
            <select
              value={memberUserId}
              onChange={e => setMemberUserId(e.target.value)}
              style={{ background: 'var(--bg-input)', border: '1px solid var(--border)', borderRadius: 'var(--radius-md)', color: 'var(--text-primary)', fontSize: 14, fontFamily: 'var(--font-display)', padding: '10px 14px', width: '100%' }}
            >
              <option value="">— Choose a user —</option>
              {allUsers.map(u => (
                <option key={u.id} value={u.id}>
                  {u.name} ({u.email}) — {u.role}
                </option>
              ))}
            </select>
          </div>
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end' }}>
            <Button variant="secondary" type="button" onClick={() => setShowMember(false)}>Cancel</Button>
            <Button variant="primary" type="submit" loading={savingMember}>Add Member</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}