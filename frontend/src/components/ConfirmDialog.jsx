import { useEffect, useRef } from 'react'

import Button from './Button.jsx'

/**
 * "Are you sure?" popup built on the browser's native <dialog> element, which already
 * handles focus trapping and the Escape key for us.
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  danger = false,
  loading = false,
  onConfirm,
  onCancel,
}) {
  const dialogRef = useRef(null)

  useEffect(() => {
    const dialog = dialogRef.current
    if (open && !dialog.open) dialog.showModal()
    if (!open && dialog.open) dialog.close()
  }, [open])

  return (
    <dialog
      ref={dialogRef}
      onCancel={onCancel}
      aria-labelledby="confirm-title"
      className="m-auto w-[min(420px,calc(100vw-32px))] rounded-card bg-card shadow-card p-5 text-ink shadow-xl backdrop:bg-ink/45"
    >
      <h2 id="confirm-title" className="mb-2 text-lg">
        {title}
      </h2>
      <p className="text-ink-soft">{message}</p>
      <div className="mt-4 flex justify-end gap-2">
        <Button variant="secondary" onClick={onCancel} disabled={loading}>
          Cancel
        </Button>
        <Button variant={danger ? 'danger' : 'primary'} onClick={onConfirm} loading={loading}>
          {confirmLabel}
        </Button>
      </div>
    </dialog>
  )
}
