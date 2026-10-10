import { createContext, useCallback, useMemo, useRef, useState } from 'react'
import { CircleAlert, CircleCheck, Info, X } from 'lucide-react'

export const NotificationContext = createContext(null)

const ICONS = { success: CircleCheck, error: CircleAlert, info: Info }
const STYLES = {
  success: 'border-l-ok-fg [&>svg]:text-ok-fg',
  error: 'border-l-danger [&>svg]:text-error-fg',
  info: 'border-l-primary [&>svg]:text-primary',
}
const DISPLAY_MS = 4500

/** Small toast system: any component can call success(), error() or info(). */
export function NotificationProvider({ children }) {
  const [toasts, setToasts] = useState([])
  const nextId = useRef(1)

  const dismiss = useCallback((id) => {
    setToasts((current) => current.filter((toast) => toast.id !== id))
  }, [])

  const show = useCallback(
    (type, message) => {
      const id = nextId.current++
      setToasts((current) => [...current, { id, type, message }])
      setTimeout(() => dismiss(id), DISPLAY_MS)
    },
    [dismiss],
  )

  const value = useMemo(
    () => ({
      success: (message) => show('success', message),
      error: (message) => show('error', message),
      info: (message) => show('info', message),
    }),
    [show],
  )

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed inset-x-4 bottom-4 z-[100] flex flex-col items-end gap-2" aria-live="polite">
        {toasts.map((toast) => {
          const Icon = ICONS[toast.type]
          return (
            <div
              key={toast.id}
              role="status"
              className={`pointer-events-auto flex w-full max-w-105 items-start gap-2.5 rounded-control border border-l-4 border-line bg-card px-3.5 py-3 text-sm shadow-lg ${STYLES[toast.type]}`}
            >
              <Icon size={18} aria-hidden="true" />
              <span className="flex-1">{toast.message}</span>
              <button
                type="button"
                className="inline-flex cursor-pointer rounded-sm p-0.5 text-ink-soft hover:text-ink"
                onClick={() => dismiss(toast.id)}
                aria-label="Dismiss"
              >
                <X size={16} aria-hidden="true" />
              </button>
            </div>
          )
        })}
      </div>
    </NotificationContext.Provider>
  )
}
