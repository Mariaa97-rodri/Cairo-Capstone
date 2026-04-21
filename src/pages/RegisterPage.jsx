import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import PrismaticBurst from '../components/ui/PrismaticBurst'
import Button from '../components/ui/Button'
import Input from '../components/ui/Input'
import styles from './AuthPage.module.css'

export default function RegisterPage() {
  const { register, loading } = useAuth()
  const navigate = useNavigate()

  const [form,   setForm]   = useState({ name: '', email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [apiErr, setApiErr] = useState('')

  const validate = () => {
    const e = {}
    if (!form.name.trim())  e.name = 'Name is required'
    if (!form.email)        e.email = 'Email is required'
    else if (!/\S+@\S+\.\S+/.test(form.email)) e.email = 'Enter a valid email'
    if (!form.password)     e.password = 'Password is required'
    else if (form.password.length < 8) e.password = 'Password must be at least 8 characters'
    return e
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setApiErr('')
    const e2 = validate()
    if (Object.keys(e2).length) { setErrors(e2); return }
    setErrors({})
    const result = await register(form.name, form.email, form.password)
    if (result.success) navigate('/dashboard')
    else setApiErr(result.error)
  }

  return (
    <div className={styles.page}>
      <div className={styles.burst}>
        <PrismaticBurst
          intensity={1.5}
          speed={0.2}
          animationType="rotate3d"
          colors={['#5f80f9', '#3529d0', '#19d9ef']}
          distort={0.5}
          hoverDampness={0}
          rayCount={0}
        />
      </div>
      <div className={styles.overlay} />

      <div className={`${styles.card} asci`}>
        <div className={styles.logo}>
          <div className={styles.logoIcon}>
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"
                stroke="var(--c-cyan)" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <span className={styles.logoText}>Cairo</span>
        </div>

        <div className={styles.prismLine} />

        <div className={styles.heading}>
          <h1 className={styles.title}>Create account</h1>
          <p className={styles.sub}>Start managing your projects</p>
        </div>

        {apiErr && (
          <div className={styles.apiError} role="alert">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/>
            </svg>
            {apiErr}
          </div>
        )}

        <form onSubmit={handleSubmit} className={styles.form} noValidate>
          <Input
            label="Full Name"
            placeholder="Maria Aguilar"
            value={form.name}
            onChange={e => setForm(f => ({ ...f, name: e.target.value }))}
            error={errors.name}
            autoComplete="name"
            required
          />
          <Input
            label="Email"
            type="email"
            placeholder="you@example.com"
            value={form.email}
            onChange={e => setForm(f => ({ ...f, email: e.target.value }))}
            error={errors.email}
            autoComplete="email"
            required
          />
          <Input
            label="Password"
            type="password"
            placeholder="Min. 8 characters"
            value={form.password}
            onChange={e => setForm(f => ({ ...f, password: e.target.value }))}
            error={errors.password}
            autoComplete="new-password"
            required
          />
          <Button type="submit" variant="cyan" size="lg" fullWidth loading={loading}>
            Create Account
          </Button>
        </form>

        <p className={styles.switch}>
          Already have an account?{' '}
          <Link to="/login" className={styles.link}>Sign in</Link>
        </p>
      </div>
    </div>
  )
}