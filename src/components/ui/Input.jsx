import styles from './Input.module.css'

export default function Input({
  label, error, id, type = 'text', placeholder,
  value, onChange, required, autoComplete, disabled, hint,
  as: Tag = 'input', rows,
}) {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')
  return (
    <div className={styles.field}>
      {label && (
        <label htmlFor={inputId} className={styles.label}>
          {label}
          {required && <span className={styles.req} aria-hidden="true"> *</span>}
        </label>
      )}
      <Tag
        id={inputId}
        type={Tag === 'input' ? type : undefined}
        rows={rows}
        className={[styles.input, error ? styles.hasError : '', Tag === 'textarea' ? styles.textarea : ''].filter(Boolean).join(' ')}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
        required={required}
        autoComplete={autoComplete}
        disabled={disabled}
        aria-describedby={error ? `${inputId}-err` : hint ? `${inputId}-hint` : undefined}
        aria-invalid={!!error}
      />
      {hint  && !error && <p id={`${inputId}-hint`} className={styles.hint}>{hint}</p>}
      {error &&           <p id={`${inputId}-err`}  className={styles.error} role="alert">{error}</p>}
    </div>
  )
}