import { useId } from 'react'

export const CONTROL_CLASSES =
  'w-full rounded-control border border-line bg-canvas px-3 py-2 text-base text-ink placeholder:text-ink-soft/70 hover:border-ink-soft focus:border-primary focus-visible:outline-primary focus-visible:outline-offset-0 disabled:cursor-not-allowed disabled:bg-sand aria-invalid:border-danger'

/**
 * Labelled text input. Pass multiline for a textarea.
 * error: message shown under the field (e.g. one entry of the backend's fieldErrors).
 */
export default function Input({ label, error, hint, multiline = false, className = '', ...rest }) {
  const id = useId()
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  const Control = multiline ? 'textarea' : 'input'

  return (
    <div className={`flex flex-col gap-1 ${className}`.trim()}>
      {label && (
        <label htmlFor={id} className="text-sm font-medium">
          {label}
          {rest.required && (
            <span className="text-danger" aria-hidden="true">
              {' '}
              *
            </span>
          )}
        </label>
      )}
      <Control
        id={id}
        className={`${CONTROL_CLASSES}${multiline ? ' min-h-24 resize-y' : ' h-10'}`}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        {...rest}
      />
      {hint && !error && (
        <p id={`${id}-hint`} className="text-sm text-ink-soft">
          {hint}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className="text-sm text-error-fg">
          {error}
        </p>
      )}
    </div>
  )
}
