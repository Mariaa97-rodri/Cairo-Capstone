import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import PrismaticBurst from '../components/ui/PrismaticBurst'
import styles from './HomePage.module.css'

// Animated counter hook
function useCounter(target, duration = 1800, start = false) {
  const ref = useRef(null)
  useEffect(() => {
    if (!start) return
    let startTime = null
    const el = ref.current
    if (!el) return
    const step = (timestamp) => {
      if (!startTime) startTime = timestamp
      const progress = Math.min((timestamp - startTime) / duration, 1)
      const ease = 1 - Math.pow(1 - progress, 3)
      el.textContent = Math.floor(ease * target).toLocaleString()
      if (progress < 1) requestAnimationFrame(step)
      else el.textContent = target.toLocaleString()
    }
    requestAnimationFrame(step)
  }, [target, duration, start])
  return ref
}

const FEATURES = [
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <rect x="3" y="3" width="5" height="18" rx="1"/>
        <rect x="10" y="3" width="5" height="12" rx="1"/>
        <rect x="17" y="3" width="5" height="15" rx="1"/>
      </svg>
    ),
    title: 'Kanban Board',
    desc: 'Visualise work across 4 stages — To Do, In Progress, In Review, and Done. Move cards with a single click.',
    color: '#5f80f9',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2"/>
        <rect x="9" y="3" width="6" height="4" rx="1"/>
        <path d="M9 12h6M9 16h4"/>
      </svg>
    ),
    title: 'Sprint Planning',
    desc: 'Create time-boxed sprints, assign issues, and track velocity. Complete sprints to auto-move unfinished work to backlog.',
    color: '#19d9ef',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
        <circle cx="9" cy="7" r="4"/>
        <path d="M23 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/>
      </svg>
    ),
    title: 'Team Collaboration',
    desc: 'Assign issues to teammates, leave comments, and get real-time notifications when something changes.',
    color: '#a78bfa',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
      </svg>
    ),
    title: 'Role-Based Access',
    desc: 'USER and ADMIN roles with fine-grained permissions. Project owners manage their teams. Admins have full control.',
    color: '#fb923c',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <circle cx="12" cy="12" r="10"/>
        <polyline points="12 6 12 12 16 14"/>
      </svg>
    ),
    title: 'Audit History',
    desc: 'Every status change is logged — who moved it, from what, to what, and when. Full transparency for every issue.',
    color: '#4ade80',
  },
  {
    icon: (
      <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
        <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 0 1-3.46 0"/>
      </svg>
    ),
    title: 'Smart Notifications',
    desc: 'Get notified when you\'re assigned an issue, added to a project, or when someone comments on your work.',
    color: '#f43f5e',
  },
]

const STEPS = [
  { num: '01', title: 'Create a Project', desc: 'Set up your project with a unique key and invite your team members.' },
  { num: '02', title: 'Plan a Sprint',    desc: 'Create a sprint, add issues from the backlog, and set your dates.' },
  { num: '03', title: 'Track Progress',   desc: 'Use the Kanban board to move issues forward as work gets done.' },
  { num: '04', title: 'Ship It',          desc: 'Complete the sprint and review what was delivered vs what wasn\'t.' },
]

export default function HomePage() {
  const navigate = useNavigate()
  const heroRef  = useRef(null)
  const statsRef = useRef(null)
  const countStarted = useRef(false)

  const c1 = useCounter(14,  1600, true)
  const c2 = useCounter(4,   1200, true)
  const c3 = useCounter(100, 2000, true)

  // Parallax on hero orbs
  useEffect(() => {
    const handleMove = (e) => {
      const hero = heroRef.current
      if (!hero) return
      const { clientX, clientY } = e
      const { width, height } = hero.getBoundingClientRect()
      const x = (clientX / width  - 0.5) * 30
      const y = (clientY / height - 0.5) * 20
      const orbs = hero.querySelectorAll('[data-orb]')
      orbs.forEach((orb, i) => {
        const factor = (i + 1) * 0.4
        orb.style.transform = `translate(${x * factor}px, ${y * factor}px)`
      })
    }
    window.addEventListener('mousemove', handleMove)
    return () => window.removeEventListener('mousemove', handleMove)
  }, [])

  // Intersection observer for fade-in sections
  useEffect(() => {
    // Small delay to ensure DOM is painted before observing
    const timer = setTimeout(() => {
      const els = document.querySelectorAll('[data-reveal]')
      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            entry.target.classList.add('revealed')
            observer.unobserve(entry.target)
          }
        })
      }, { threshold: 0.05, rootMargin: '0px 0px -40px 0px' })
      els.forEach(el => observer.observe(el))
      // Also immediately reveal any sections already in viewport
      els.forEach(el => {
        const rect = el.getBoundingClientRect()
        if (rect.top < window.innerHeight) {
          el.classList.add('revealed')
        }
      })
    }, 100)
    return () => clearTimeout(timer)
  }, [])

  return (
    <div className={styles.page}>

      {/* ── NAV ── */}
      <nav className={styles.nav}>
        <div className={styles.navLogo}>
          <div className={styles.navLogoMark}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
                stroke="#19d9ef" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <span className={styles.navLogoText}>Cairo</span>
        </div>
        <div className={styles.navActions}>
          <button className={styles.navLogin}    onClick={() => navigate('/login')}>Sign In</button>
          <button className={styles.navRegister} onClick={() => navigate('/register')}>Get Started</button>
        </div>
      </nav>

      {/* ── HERO ── */}
      <section className={styles.hero} ref={heroRef}>
        {/* Animated burst background */}
        <div className={styles.burst}>
          <PrismaticBurst
            intensity={1.2}
            speed={0.15}
            animationType="rotate3d"
            colors={['#5f80f9', '#3529d0', '#19d9ef']}
            distort={0.4}
            hoverDampness={0}
            rayCount={0}
          />
        </div>
        <div className={styles.heroOverlay} />

        {/* Floating orbs */}
        <div className={styles.orb1} data-orb />
        <div className={styles.orb2} data-orb />
        <div className={styles.orb3} data-orb />

        {/* Hero content */}
        <div className={styles.heroContent}>
          <div className={styles.heroBadge}>
            <span className={styles.heroBadgeDot} />
            Self-hosted · Open codebase · Zero per-seat cost
          </div>

          <h1 className={styles.heroTitle}>
            Project management
            <br />
            <span className={styles.heroTitleAccent}>built for your team</span>
          </h1>

          <p className={styles.heroSub}>
            Cairo is a lightweight Jira alternative — sprints, Kanban boards,
            issue tracking, and team collaboration. All on your own infrastructure.
          </p>

          <div className={styles.heroCtas}>
            <button className={styles.ctaPrimary} onClick={() => navigate('/register')}>
              <span>Create Free Account</span>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
            <button className={styles.ctaSecondary} onClick={() => navigate('/login')}>
              Sign In
            </button>
          </div>

          <p className={styles.heroHint}>No credit card. No vendor lock-in. Your data stays yours.</p>
        </div>

        {/* Hero visual — mini Kanban preview */}
        <div className={styles.heroVisual}>
          <div className={styles.kanbanPreview}>
            <div className={styles.kanbanHeader}>
              <span className={styles.kDot} style={{ background:'#454d7a' }} />
              <span className={styles.kCol}>To Do</span>
              <span className={styles.kCount}>3</span>
            </div>
            {['Set up auth endpoints','Write unit tests','Update README'].map((t, i) => (
              <div key={i} className={styles.kCard} style={{ animationDelay: `${i * 0.15}s` }}>
                <span className={styles.kTag} style={{ color: i === 0 ? '#f43f5e' : i === 1 ? '#5f80f9' : '#19d9ef', borderColor: i === 0 ? '#f43f5e44' : i === 1 ? '#5f80f944' : '#19d9ef44' }}>
                  {i === 0 ? 'BUG' : i === 1 ? 'TASK' : 'TASK'}
                </span>
                <p className={styles.kTitle}>{t}</p>
                <div className={styles.kAvatar}>{['A','M','J'][i]}</div>
              </div>
            ))}
          </div>
          <div className={styles.kanbanPreview} style={{ marginTop: 24 }}>
            <div className={styles.kanbanHeader}>
              <span className={styles.kDot} style={{ background:'#5f80f9' }} />
              <span className={styles.kCol}>In Progress</span>
              <span className={styles.kCount}>2</span>
            </div>
            {['Build Kanban board UI','JWT authentication'].map((t, i) => (
              <div key={i} className={styles.kCard} style={{ animationDelay: `${(i + 3) * 0.15}s`, borderColor: 'rgba(95,128,249,0.25)' }}>
                <span className={styles.kTag} style={{ color:'#a78bfa', borderColor:'#a78bfa44' }}>
                  STORY
                </span>
                <p className={styles.kTitle}>{t}</p>
                <div className={styles.kAvatar} style={{ background:'linear-gradient(135deg,#3529d0,#19d9ef)' }}>{['S','J'][i]}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── STATS ── */}
      <section className={styles.stats} ref={statsRef} data-reveal>
        {[
          { ref: c1, suffix: '+', label: 'API Endpoints',    sub: 'Fully tested REST API' },
          { ref: c2, suffix: ' roles', label: 'User Roles',  sub: 'USER and ADMIN access' },
          { ref: c3, suffix: '%',  label: 'Self-hosted',     sub: 'Your data, your server' },
        ].map((s, i) => (
          <div key={i} className={styles.statCard}>
            <div className={styles.statNum}>
              <span ref={s.ref}>0</span>
              <span className={styles.statSuffix}>{s.suffix}</span>
            </div>
            <div className={styles.statLabel}>{s.label}</div>
            <div className={styles.statSub}>{s.sub}</div>
          </div>
        ))}
      </section>

      {/* ── FEATURES ── */}
      <section className={styles.features} data-reveal>
        <div className={styles.sectionHeader}>
          <p className={styles.sectionEyebrow}>Everything you need</p>
          <h2 className={styles.sectionTitle}>Built for real teams</h2>
          <p className={styles.sectionSub}>
            Cairo ships with all the core features your team needs to plan, track, and ship work — without the enterprise price tag.
          </p>
        </div>
        <div className={styles.featuresGrid}>
          {FEATURES.map((f, i) => (
            <div key={i} className={styles.featureCard} style={{ '--accent': f.color, animationDelay: `${i * 0.07}s` }}>
              <div className={styles.featureIcon} style={{ color: f.color, background: `${f.color}18`, borderColor: `${f.color}30` }}>
                {f.icon}
              </div>
              <h3 className={styles.featureTitle}>{f.title}</h3>
              <p className={styles.featureDesc}>{f.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ── HOW IT WORKS ── */}
      <section className={styles.howto} data-reveal>
        <div className={styles.sectionHeader}>
          <p className={styles.sectionEyebrow}>Get started in minutes</p>
          <h2 className={styles.sectionTitle}>How Cairo works</h2>
        </div>
        <div className={styles.steps}>
          {STEPS.map((s, i) => (
            <div key={i} className={styles.step}>
              <div className={styles.stepNum}>{s.num}</div>
              {i < STEPS.length - 1 && <div className={styles.stepLine} />}
              <div className={styles.stepBody}>
                <h3 className={styles.stepTitle}>{s.title}</h3>
                <p className={styles.stepDesc}>{s.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* ── TECH STACK ── */}
      <section className={styles.stack} data-reveal>
        <p className={styles.stackLabel}>Built with</p>
        <div className={styles.stackPills}>
          {['React 18','Spring Boot 4','MySQL 8','JWT Auth','REST API','JPA/Hibernate'].map((t, i) => (
            <span key={i} className={styles.stackPill}>{t}</span>
          ))}
        </div>
      </section>

      {/* ── CTA BANNER ── */}
      <section className={styles.ctaBanner} data-reveal>
        <div className={styles.ctaBannerBg} />
        <div className={styles.ctaBannerContent}>
          <h2 className={styles.ctaBannerTitle}>Ready to ship faster?</h2>
          <p className={styles.ctaBannerSub}>
            Create your account and start your first sprint in under 5 minutes.
          </p>
          <div className={styles.ctaBannerBtns}>
            <button className={styles.ctaPrimary} onClick={() => navigate('/register')}>
              <span>Create Free Account</span>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M5 12h14M12 5l7 7-7 7"/>
              </svg>
            </button>
            <button className={styles.ctaSecondary} onClick={() => navigate('/login')}>
              Already have an account? Sign In
            </button>
          </div>
        </div>
      </section>

      {/* ── FOOTER ── */}
      <footer className={styles.footer}>
        <div className={styles.footerLogo}>
          <div className={styles.navLogoMark}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
                stroke="#19d9ef" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <span className={styles.navLogoText}>Cairo</span>
        </div>
        <p className={styles.footerSub}>
          UCI 2123 Capstone · Maria Aguilar · 2026
        </p>
      </footer>
    </div>
  )
}