import { Link } from 'react-router-dom'
import { CircleCheck, CircleX, Clock } from 'lucide-react'

import Button from './Button.jsx'
import StatusBadge from './StatusBadge.jsx'
import { formatDate } from '../utils/format.js'

const STATUS_ICONS = { PENDING: Clock, APPROVED: CircleCheck, REJECTED: CircleX }

// "today", "1 day ago", "5 days ago"
function ageLabel(value) {
  const days = Math.floor((Date.now() - new Date(value).getTime()) / 86_400_000)
  if (days <= 0) return 'today'
  return `${days} ${days === 1 ? 'day' : 'days'} ago`
}

// What the claim status means for the person who made the claim, in plain words.
function outcome(claim) {
  if (claim.status === 'APPROVED') {
    return claim.itemStatus === 'RETURNED'
      ? 'Approved. The item has been handed over to you.'
      : 'Approved. Collect the item from the admin office; it will then be marked as returned.'
  }
  if (claim.status === 'REJECTED') return 'Not approved. Another claim was chosen, or the report was closed.'
  return 'Waiting for an admin to review your claim.'
}

/** One label/value row of the claim details. */
function Row({ label, children }) {
  return (
    <div className="grid gap-1 sm:grid-cols-[9rem_1fr] sm:gap-4">
      <dt className="text-sm text-ink-soft">{label}</dt>
      <dd className="text-sm text-ink">{children}</dd>
    </div>
  )
}

/**
 * Shows one ClaimResponse as: header (item, submitted date, claim status) -> details
 * (message and outcome, or claimant and item status for admins) -> footer actions.
 * children: optional buttons (used by the admin queue) that replace the default "View item" link.
 * showClaimant: the admin sees who is claiming; the claimant sees what the status means for them.
 */
export default function ClaimCard({ claim, showClaimant = false, children }) {
  const headingId = `claim-${claim.id}-title`

  return (
    <article aria-labelledby={headingId} className="overflow-hidden rounded-md border border-line bg-card">
      <header className="flex flex-wrap items-start justify-between gap-x-4 gap-y-2 border-b border-line bg-sand px-4 py-3">
        <div>
          <h3 id={headingId}>
            <Link to={`/items/${claim.itemId}`} className="text-ink">
              {claim.itemName}
            </Link>
          </h3>
          <p className="mt-0.5 text-sm text-ink-soft">
            Submitted <time dateTime={claim.createdAt}>{formatDate(claim.createdAt)}</time> ({ageLabel(claim.createdAt)})
          </p>
        </div>
        <StatusBadge value={claim.status} icon={STATUS_ICONS[claim.status]} />
      </header>

      <dl className="flex flex-col gap-3 px-4 py-4">
        {showClaimant && <Row label="Claimant">{claim.claimantName}</Row>}
        <Row label={showClaimant ? 'Message' : 'Your message'}>
          <span className="whitespace-pre-line">{claim.message}</span>
        </Row>
        {showClaimant ? (
          <Row label="Item status">
            <StatusBadge value={claim.itemStatus} />
          </Row>
        ) : (
          <Row label="What this means">{outcome(claim)}</Row>
        )}
      </dl>

      <footer className="flex flex-wrap justify-end gap-2 border-t border-line px-4 py-3">
        {children ?? (
          <Button size="sm" variant="secondary" to={`/items/${claim.itemId}`}>
            View item
          </Button>
        )}
      </footer>
    </article>
  )
}
