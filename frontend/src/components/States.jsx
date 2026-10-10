import { CircleAlert, Inbox, LoaderCircle } from 'lucide-react'

import Button from './Button.jsx'

export function LoadingState({ message = 'Loading...' }) {
  return (
    <div className="flex flex-col items-center gap-2 px-4 py-8 text-center text-ink-soft" role="status">
      <LoaderCircle className="animate-spin" size={24} aria-hidden="true" />
      <p>{message}</p>
    </div>
  )
}

// action: optional { label, to } rendered as a button
export function EmptyState({ title, message, action }) {
  return (
    <div className="flex flex-col items-center gap-2 rounded-card bg-card shadow-card px-4 py-8 text-center text-ink-soft">
      <Inbox size={28} aria-hidden="true" />
      <h3 className="text-ink">{title}</h3>
      {message && <p>{message}</p>}
      {action && (
        <Button to={action.to} className="mt-2">
          {action.label}
        </Button>
      )}
    </div>
  )
}

// Inline error box. onRetry (optional) shows a "Try again" button.
export function ErrorMessage({ message, onRetry }) {
  return (
    <div className="flex flex-wrap items-center gap-2 rounded-control bg-error-bg px-4 py-2 text-sm text-error-fg" role="alert">
      <CircleAlert size={18} aria-hidden="true" />
      <p className="min-w-40 flex-1">{message}</p>
      {onRetry && (
        <Button variant="secondary" size="sm" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}
