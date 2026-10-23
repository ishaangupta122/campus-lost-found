import { useId } from 'react'

import { CONTROL_CLASSES } from './Input.jsx'

// Pass <option> elements as children.
export default function Select({ label, error, className = '', children, ...rest }) {
  const id = useId()

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
      <select
        id={id}
        className={`${CONTROL_CLASSES} h-10`}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : undefined}
        {...rest}
      >
        {children}
      </select>
      {error && (
        <p id={`${id}-error`} className="text-sm text-error-fg">
          {error}
        </p>
      )}
    </div>
  )
}
