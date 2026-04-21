import styles from './Button.module.css'

export default function Button({
  children, variant = 'primary', size = 'md',
  loading = false, disabled = false, type = 'button',
  onClick, fullWidth = false, 'aria-label': ariaLabel,
}) {
  return (
    <button
      type={type}
      className={[styles.btn, styles[variant], styles[size],
        fullWidth ? styles.fullWidth : '',
        loading   ? styles.isLoading : '',
      ].filter(Boolean).join(' ')}
      disabled={disabled || loading}
      onClick={onClick}
      aria-label={ariaLabel}
      aria-busy={loading}
    >
      {loading && <span className={styles.spinner} aria-hidden="true" />}
      <span style={{ visibility: loading ? 'hidden' : 'visible' }}>{children}</span>
    </button>
  )
}