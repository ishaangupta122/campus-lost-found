import { Link } from 'react-router-dom'

import ItemImage from './ItemImage.jsx'
import StatusBadge from './StatusBadge.jsx'
import { formatDate } from '../utils/format.js'

/**
 * Compact table version of the item list: one row per report, so many items fit on a screen.
 * Less important columns are hidden on narrow screens (their info moves under the item name).
 */
export default function ItemTable({ items, dimmed = false }) {
  return (
    <div className={`overflow-x-auto rounded-card bg-card shadow-card ${dimmed ? 'opacity-60' : ''}`}>
      <table className="w-full text-left text-sm">
        <thead className="border-b border-line bg-sand text-xs tracking-wide text-ink-soft uppercase">
          <tr>
            <th className="px-3 py-2 font-medium">Item</th>
            <th className="px-3 py-2 font-medium">Type</th>
            <th className="hidden px-3 py-2 font-medium md:table-cell">Category</th>
            <th className="hidden px-3 py-2 font-medium sm:table-cell">Location</th>
            <th className="hidden px-3 py-2 font-medium sm:table-cell">Date</th>
            <th className="hidden px-3 py-2 font-medium md:table-cell">Status</th>
            <th className="px-3 py-2 text-right font-medium">
              <span className="sr-only">Action</span>
            </th>
          </tr>
        </thead>
        <tbody className="divide-y divide-line">
          {items.map((item) => {
            const claimable = item.type === 'FOUND' && (item.status === 'OPEN' || item.status === 'CLAIM_PENDING')
            return (
              <tr key={item.id} className="align-middle hover:bg-sand/50">
                <td className="px-3 py-1.5">
                  <div className="flex items-center gap-3">
                    <ItemImage imagePath={item.imagePath} className="size-9 shrink-0 rounded-md bg-sand object-cover" />
                    <div className="min-w-0">
                      <Link to={`/items/${item.id}`} className="block truncate font-medium text-ink">
                        {item.itemName}
                      </Link>
                      <p className="truncate text-xs text-ink-soft sm:hidden">
                        {item.location} · {formatDate(item.reportDate)}
                      </p>
                    </div>
                  </div>
                </td>
                <td className="px-3 py-1.5">
                  <StatusBadge value={item.type} />
                </td>
                <td className="hidden px-3 py-1.5 text-ink-soft md:table-cell">{item.category}</td>
                <td className="hidden max-w-48 truncate px-3 py-1.5 text-ink-soft sm:table-cell">{item.location}</td>
                <td className="hidden px-3 py-1.5 whitespace-nowrap text-ink-soft sm:table-cell">
                  {formatDate(item.reportDate)}
                </td>
                <td className="hidden px-3 py-1.5 md:table-cell">
                  <StatusBadge value={item.status} />
                </td>
                <td className="px-3 py-1.5 text-right whitespace-nowrap">
                  <Link to={claimable ? `/items/${item.id}#claim` : `/items/${item.id}`} className="text-sm font-medium">
                    {claimable ? 'Claim' : 'View'}
                  </Link>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
