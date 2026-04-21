import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { projectService } from '../services'
import Topbar from '../components/layout/Topbar'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import Modal from '../components/ui/Modal'
import { Skeleton } from '../components/ui'
import styles from './ProjectsPage.module.css'

export default function ProjectsPage() {
  const navigate  = useNavigate()
  const [projects,  setProjects]  = useState([])
  const [loading,   setLoading]   = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [saving,    setSaving]    = useState(false)
  const [apiErr,    setApiErr]    = useState('')
  const [form,      setForm]      = useState({ name: '', projectKey: '', description: '' })
  const [errors,    setErrors]    = useState({})

  const load = useCallback(() => {
    setLoading(true)
    projectService.getAll(0, 50)
      .then(r => setProjects(r.data.content || []))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const validate = () => {
    const e = {}
    if (!form.name.trim())       e.name       = 'Project name is required'
    if (!form.projectKey.trim()) e.projectKey = 'Project key is required'
    else if (form.projectKey.length > 10) e.projectKey = 'Key cannot exceed 10 characters'
    else if (!/^[A-Za-z0-9]+$/.test(form.projectKey)) e.projectKey = 'Letters and numbers only'
    return e
  }

  const handleCreate = async (e) => {
    e.preventDefault()
    setApiErr('')
    const errs = validate()
    if (Object.keys(errs).length) { setErrors(errs); return }
    setErrors({})
    setSaving(true)
    try {
      await projectService.create({ ...form, projectKey: form.projectKey.toUpperCase() })
      setShowModal(false)
      setForm({ name: '', projectKey: '', description: '' })
      load()
    } catch (err) {
      setApiErr(err.response?.data?.message || 'Failed to create project.')
    } finally {
      setSaving(false)
    }
  }

  const closeModal = () => {
    setShowModal(false)
    setForm({ name: '', projectKey: '', description: '' })
    setErrors({})
    setApiErr('')
  }

  return (
    <div>
      <Topbar title="Projects" subtitle="All your workspaces in one place" />

      <div className="page-header">
        <div />
        <Button variant="cyan" onClick={() => setShowModal(true)}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
            <path d="M12 5v14M5 12h14"/>
          </svg>
          New Project
        </Button>
      </div>

      {loading ? (
        <div className={styles.grid}>
          {[1,2,3,4,5,6].map(i => (
            <div key={i} className={styles.card}>
              <Skeleton height={14} width="40%" />
              <Skeleton height={20} width="70%" style={{ marginTop: 10 }} />
              <Skeleton height={14} width="90%" style={{ marginTop: 8 }} />
              <Skeleton height={14} width="60%" style={{ marginTop: 4 }} />
            </div>
          ))}
        </div>
      ) : projects.length === 0 ? (
        <div className="empty-state afu">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.2">
            <rect x="2" y="7" width="20" height="14" rx="2"/>
            <path d="M16 7V5a2 2 0 0 0-4 0v2M8 7V5a2 2 0 0 0-4 0v2"/>
          </svg>
          <h3>No projects yet</h3>
          <p>Create your first project and start tracking work</p>
          <Button variant="primary" onClick={() => setShowModal(true)}>Create Project</Button>
        </div>
      ) : (
        <div className={styles.grid}>
          {projects.map((p, i) => (
            <div
              key={p.id}
              className={`${styles.card} afu-${Math.min(i+1,5)}`}
              onClick={() => navigate(`/projects/${p.id}`)}
              role="button" tabIndex={0}
              onKeyDown={e => e.key === 'Enter' && navigate(`/projects/${p.id}`)}
              aria-label={`Open ${p.name}`}
            >
              <div className={styles.keyBadge}>{p.projectKey}</div>
              <h3 className={styles.cardName}>{p.name}</h3>
              {p.description && <p className={styles.cardDesc}>{p.description}</p>}
              <div className={styles.cardFooter}>
                <span className={styles.ownerLabel}>
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  {p.owner?.name}
                </span>
                <span className={styles.arrow}>→</span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create Modal */}
      <Modal open={showModal} onClose={closeModal} title="New Project">
        {apiErr && (
          <div style={{ background: 'rgba(244,63,94,0.1)', border: '1px solid rgba(244,63,94,0.25)', borderRadius: 'var(--radius-md)', padding: '10px 14px', fontSize: 13, color: '#f43f5e' }}>
            {apiErr}
          </div>
        )}
        <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: 14 }} noValidate>
          <Input
            label="Project Name" required
            placeholder="e.g. Cairo Backend"
            value={form.name}
            onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
            error={errors.name}
          />
          <Input
            label="Project Key" required
            placeholder="e.g. CAIRO"
            value={form.projectKey}
            onChange={e => setForm(f => ({ ...f, projectKey: e.target.value.toUpperCase() }))}
            error={errors.projectKey}
            hint="Short identifier used in issue IDs. Max 10 characters."
          />
          <Input
            label="Description"
            as="textarea" rows={3}
            placeholder="What is this project about?"
            value={form.description}
            onChange={e => setForm(f => ({ ...f, description: e.target.value }))}
          />
          <div style={{ display: 'flex', gap: 10, justifyContent: 'flex-end', paddingTop: 4 }}>
            <Button variant="secondary" onClick={closeModal} type="button">Cancel</Button>
            <Button variant="cyan" type="submit" loading={saving}>Create Project</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}