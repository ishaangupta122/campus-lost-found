import { Link } from 'react-router-dom'
import { Calendar, MapPin } from 'lucide-react'

import Button from './Button.jsx'
import ItemImage from './ItemImage.jsx'
import StatusBadge from './StatusBadge.jsx'
import { formatDate } from '../utils/format.js'

/**
 * Shows one ItemResponse from the backend. The title link stretches over the whole card
 * (after:absolute), so the card is one big click target. The button at the bottom sits
 * above that link: "Claim this item" for a found item that is still waiting for its
 * owner, "View details" for everything else.
 * children: optional action buttons that replace the default button (used on "My items").
 */
export default function ItemCard({ item, children }) {
  const claimable = item.type === 'FOUND' && (item.status === 'OPEN' || item.status === 'CLAIM_PENDING')

  return (
    <article className="relative flex flex-col overflow-hidden rounded-card bg-card shadow-card transition-colors hover:shadow-md has-[a:focus-visible]:outline-2 has-[a:focus-visible]:outline-offset-2 has-[a:focus-visible]:outline-primary">
      <div className="h-32 bg-sand">
        <ItemImage imagePath={item.imagePath} className="size-full object-cover" />
      </div>

      <div className="flex flex-1 flex-col gap-2 p-3">
        <div className="flex items-start justify-between gap-2">
          <h3 className="min-w-0 break-words">
            <Link
              to={`/items/${item.id}`}
              className="text-ink after:absolute after:inset-0 hover:no-underline focus-visible:outline-none"
            >
              {item.itemName}
            </Link>
          </h3>
          <StatusBadge value={item.status} />
        </div>

        <div className="flex flex-wrap gap-1.5">
          <StatusBadge value={item.type} />
          <span className="inline-flex items-center rounded-full bg-sand px-2 py-0.5 text-xs font-medium text-ink-soft">
            {item.category}
          </span>
        </div>

        <ul className="flex flex-col gap-1 text-sm text-ink-soft">
          <li className="flex items-center gap-2">
            <MapPin size={14} className="shrink-0" aria-hidden="true" />
            <span className="truncate">{item.location}</span>
          </li>
          <li className="flex items-center gap-2">
            <Calendar size={14} className="shrink-0" aria-hidden="true" />
            {item.type === 'FOUND' ? 'Found' : 'Lost'}: {formatDate(item.reportDate)}
          </li>
        </ul>

        <div className="relative z-10 mt-auto flex flex-wrap gap-2 pt-1">
          {children ?? (
            <Button
              to={claimable ? `/items/${item.id}#claim` : `/items/${item.id}`}
              variant={claimable ? 'primary' : 'secondary'}
              className="w-full"
            >
              {claimable ? 'Claim this item' : 'View details'}
            </Button>
          )}
        </div>
      </div>
    </article>
  )
}
