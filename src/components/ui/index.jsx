/* ── Badge ── */
export function Badge({ type, value, className = '' }) {
  const key = value?.toLowerCase().replace(/_/g, '_') || ''
  return (
    <span className={`badge badge-${key} ${className}`}>
      {value?.replace(/_/g, ' ')}
    </span>
  )
}

/* ── Spinner ── */
export function Spinner({ size = 20, color = 'var(--c-blue)' }) {
  return (
    <svg
      width={size} height={size}
      viewBox="0 0 24 24" fill="none"
      style={{ animation: 'spin 0.7s linear infinite', flexShrink: 0 }}
      aria-label="Loading"
    >
      <circle cx="12" cy="12" r="10" stroke={color} strokeWidth="2.5" strokeOpacity="0.2" />
      <path d="M12 2a10 10 0 0 1 10 10" stroke={color} strokeWidth="2.5" strokeLinecap="round" />
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </svg>
  )
}

/* ── Skeleton ── */
export function Skeleton({ width = '100%', height = 16, className = '' }) {
  return (
    <div
      className={`skeleton ${className}`}
      style={{ width, height }}
      aria-hidden="true"
    />
  )
}

/* ── Select ── */
import styles from './Select.module.css'

export function Select({ label, value, onChange, options, required, error, id, disabled }) {
  const selectId = id || label?.toLowerCase().replace(/\s+/g, '-')
  return (
    <div className={styles.field}>
      {label && (
        <label htmlFor={selectId} className={styles.label}>
          {label}{required && <span style={{ color: 'var(--c-cyan)' }}> *</span>}
        </label>
      )}
      <select
        id={selectId}
        value={value}
        onChange={onChange}
        required={required}
        disabled={disabled}
        className={[styles.select, error ? styles.hasError : ''].filter(Boolean).join(' ')}
        aria-invalid={!!error}
      >
        {options.map(opt => (
          <option key={opt.value} value={opt.value}>{opt.label}</option>
        ))}
      </select>
      {error && <p className={styles.error} role="alert">{error}</p>}
    </div>
  )
}