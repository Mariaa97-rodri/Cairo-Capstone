import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { projectService, issueService, sprintService } from '../services'
import Topbar from '../components/layout/Topbar'
import { Skeleton } from '../components/ui'
import styles from './DashboardPage.module.css'

function StatCard({ label, value, color, loading, onClick }) {
  return (
    <div
      className={styles.statCard}
      style={{ '--accent': color, cursor: onClick ? 'pointer' : 'default' }}
      onClick={onClick}
      role={onClick ? 'button' : undefined}
      tabIndex={onClick ? 0 : undefined}
      onKeyDown={onClick ? e => e.key === 'Enter' && onClick() : undefined}
    >
      {loading
        ? <Skeleton height={36} width="60%" />
        : <span className={styles.statValue}>{value}</span>
      }
      <span className={styles.statLabel}>{label}</span>
      <div className={styles.statLine} />
    </div>
  )
}

export default function DashboardPage() {
  const { user } = useAuth()
  const navigate  = useNavigate()

  const [projects,      setProjects]      = useState([])
  const [openIssues,    setOpenIssues]    = useState(0)
  const [activeSprints, setActiveSprints] = useState(0)
  const [loading,       setLoading]       = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const pRes = await projectService.getAll(0, 50)
        const allProjects = pRes.data.content || []
        setProjects(allProjects)

        // For each project, fetch sprints and issues in parallel
        const results = await Promise.all(
          allProjects.map(p =>
            Promise.all([
              sprintService.getAll(p.id).catch(() => ({ data: [] })),
              issueService.getAll(p.id, { status: 'TODO' }, 0, 100).catch(() => ({ data: { content: [] } })),
              issueService.getAll(p.id, { status: 'IN_PROGRESS' }, 0, 100).catch(() => ({ data: { content: [] } })),
              issueService.getAll(p.id, { status: 'IN_REVIEW' }, 0, 100).catch(() => ({ data: { content: [] } })),
            ])
          )
        )

        let totalOpen = 0
        let totalActive = 0
        results.forEach(([sprintRes, todoRes, inProgRes, inRevRes]) => {
          const sprints = sprintRes.data || []
          totalActive += sprints.filter(s => s.status === 'ACTIVE').length
          totalOpen   += (todoRes.data.content?.length   || 0)
          totalOpen   += (inProgRes.data.content?.length || 0)
          totalOpen   += (inRevRes.data.content?.length  || 0)
        })

        setActiveSprints(totalActive)
        setOpenIssues(totalOpen)
      } catch {}
      finally { setLoading(false) }
    }
    load()
  }, [])

  const hour = new Date().getHours()
  const greeting = hour < 12 ? 'Good morning' : hour < 17 ? 'Good afternoon' : 'Good evening'

  return (
    <div>
      <Topbar
        title={`${greeting}, ${user?.name?.split(' ')[0]} 👋`}
        subtitle="Here's what's happening across your projects"
      />

      <div className={`${styles.statsGrid} afu`}>
        <StatCard
          label="Total Projects"
          value={projects.length}
          color="var(--c-blue)"
          loading={loading}
          onClick={() => navigate('/projects')}
        />
        <StatCard
          label="Active Sprints"
          value={activeSprints}
          color="var(--c-cyan)"
          loading={loading}
        />
        <StatCard
          label="Open Issues"
          value={openIssues}
          color="#fb923c"
          loading={loading}
        />
        <StatCard
          label="My Role"
          value={user?.role || '—'}
          color="var(--c-indigo)"
          loading={loading}
        />
      </div>

      {/* Recent projects */}
      <section className={`${styles.section} afu-2`}>
        <div className="flex-between" style={{ marginBottom: 16 }}>
          <h2 className={styles.sectionTitle}>Recent Projects</h2>
          <button className={styles.seeAll} onClick={() => navigate('/projects')}>
            See all →
          </button>
        </div>

        {loading ? (
          <div className={styles.projectsGrid}>
            {[1,2,3].map(i => (
              <div key={i} className={styles.projectCard}>
                <Skeleton height={20} width="70%" />
                <Skeleton height={14} width="40%" style={{ marginTop: 8 }} />
                <Skeleton height={14} width="55%" style={{ marginTop: 6 }} />
              </div>
            ))}
          </div>
        ) : projects.length === 0 ? (
          <div className="empty-state">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
              <path d="M2 7h20M2 12h20M2 17h20"/>
            </svg>
            <h3>No projects yet</h3>
            <p>Create your first project to get started</p>
            <button className={styles.createBtn} onClick={() => navigate('/projects')}>
              Create Project
            </button>
          </div>
        ) : (
          <div className={styles.projectsGrid}>
            {projects.slice(0, 6).map((p, i) => (
              <div
                key={p.id}
                className={`${styles.projectCard} afu-${Math.min(i + 1, 5)}`}
                onClick={() => navigate(`/projects/${p.id}`)}
                role="button" tabIndex={0}
                onKeyDown={e => e.key === 'Enter' && navigate(`/projects/${p.id}`)}
                aria-label={`Open project ${p.name}`}
              >
                <div className={styles.projectKey}>{p.projectKey}</div>
                <h3 className={styles.projectName}>{p.name}</h3>
                {p.description && <p className={styles.projectDesc}>{p.description}</p>}
                <div className={styles.projectMeta}>
                  <span className={styles.owner}>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
                    </svg>
                    {p.owner?.name}
                  </span>
                  <span className={styles.viewBtn}>View →</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}