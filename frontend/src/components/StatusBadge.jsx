import { formatLabel } from '../utils/format.js'

// Item status, item type or claim status -> one small colored label.
// (Class names are written out in full so Tailwind can find them.)
const STYLES = {
  OPEN: 'bg-ok-bg text-ok-fg',
  CLAIM_PENDING: 'bg-warn-bg text-warn-fg',
  RETURNED: 'bg-info-bg text-info-fg',
  CLOSED: 'bg-stone-bg text-stone-fg',
  LOST: 'bg-lost-bg text-lost-fg',
  FOUND: 'bg-found-bg text-found-fg',
  PENDING: 'bg-warn-bg text-warn-fg',
  APPROVED: 'bg-ok-bg text-ok-fg',
  REJECTED: 'bg-error-bg text-error-fg',
}

export default function StatusBadge({ value }) {
  const style = STYLES[value] ?? STYLES.CLOSED
  return (
    <span className={`inline-block whitespace-nowrap rounded-md px-2 py-0.5 text-xs font-medium leading-normal ${style}`}>
      {formatLabel(value)}
    </span>
  )
}
