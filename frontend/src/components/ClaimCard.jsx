import { Link } from 'react-router-dom'
import { Clock, User } from 'lucide-react'

import StatusBadge from './StatusBadge.jsx'
import { formatDate } from '../utils/format.js'

// "Today", "1 day ago", "5 days ago"
function ageLabel(value) {
  const days = Math.floor((Date.now() - new Date(value).getTime()) / 86_400_000)
  if (days <= 0) return 'Today'
  return `${days} ${days === 1 ? 'day' : 'days'} ago`
}

/**
 * Shows one ClaimResponse. children: optional buttons (used by the admin queue).
 * showClaimant: the admin needs to see who is claiming; the claimant already knows.
 */
export default function ClaimCard({ claim, showClaimant = false, children }) {
  return (
    <article className="flex flex-col gap-3 rounded-card bg-card p-4 shadow-card">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3>
          <Link to={`/items/${claim.itemId}`} className="text-ink">
            {claim.itemName}
          </Link>
        </h3>
        <StatusBadge value={claim.status} />
      </div>

      <p className="border-l-2 border-primary/30 pl-3 text-sm whitespace-pre-line text-ink">{claim.message}</p>

      <p className="flex flex-wrap items-center gap-x-2 gap-y-1 text-xs text-ink-soft">
        {showClaimant && (
          <>
            <span className="inline-flex items-center gap-1">
              <User size={12} aria-hidden="true" /> {claim.claimantName}
            </span>
            <span aria-hidden="true">·</span>
          </>
        )}
        <span className="inline-flex items-center gap-1" title={formatDate(claim.createdAt)}>
          <Clock size={12} aria-hidden="true" /> {ageLabel(claim.createdAt)}
        </span>
        <span aria-hidden="true">·</span>
        <span className="inline-flex items-center gap-1.5">
          Item <StatusBadge value={claim.itemStatus} />
        </span>
      </p>

      {children && <div className="flex flex-wrap gap-2">{children}</div>}
    </article>
  )
}
