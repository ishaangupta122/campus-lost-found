import { Link, useOutletContext } from 'react-router-dom'
import { Archive, CircleCheck, Clock, FileSearch, HandHelping, PackageCheck, PackageOpen, Percent, Users } from 'lucide-react'

import Button from '../components/Button.jsx'
import PageHeader from '../components/PageHeader.jsx'
import Panel, { PanelLink } from '../components/Panel.jsx'
import StatCard from '../components/StatCard.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { ErrorMessage, LoadingState } from '../components/States.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { getPendingClaims, listAllItems } from '../services/adminService.js'
import { formatDate } from '../utils/format.js'

// Segments of the "items by status" bar: shades of the brand red, plus green and grey. (Full class names so Tailwind finds them.)
const STATUS_BARS = [
  { key: 'openItems', label: 'Open', color: 'bg-primary' },
  { key: 'claimPendingItems', label: 'Claim pending', color: 'bg-primary/50' },
  { key: 'returnedItems', label: 'Returned', color: 'bg-ok-fg' },
  { key: 'closedItems', label: 'Closed', color: 'bg-stone-fg/35' },
]

export default function AdminDashboard() {
  usePageTitle('Admin dashboard')
  const { stats, statsError, reloadStats } = useOutletContext()
  const { data: pending } = useFetch(() => getPendingClaims(), [])
  const { data: allItems } = useFetch(() => listAllItems({}), [])

  if (statsError) return <ErrorMessage message={statsError} onRetry={reloadStats} />
  if (!stats) return <LoadingState message="Loading dashboard..." />

  const totalItems = stats.openItems + stats.claimPendingItems + stats.returnedItems + stats.closedItems
  const returnRate = totalItems > 0 ? Math.round((stats.returnedItems / totalItems) * 100) : 0
  const latest = [...(allItems ?? [])].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 5)

  return (
    <div className="flex flex-col gap-6">
      <PageHeader title="Admin dashboard" subtitle="A quick look at what is happening on campus." />

      {/* What needs the admin's attention right now */}
      <section
        className={`flex flex-wrap items-center justify-between gap-3 rounded-md border px-4 py-3 ${
          stats.pendingClaims > 0 ? 'border-warn-fg/30 bg-warn-bg' : 'border-line bg-card'
        }`}
      >
        <div className="flex items-center gap-3">
          {stats.pendingClaims > 0 ? (
            <Clock size={24} className="text-warn-fg" aria-hidden="true" />
          ) : (
            <CircleCheck size={24} className="text-ok-fg" aria-hidden="true" />
          )}
          <div>
            <h2 className="text-base">
              {stats.pendingClaims > 0
                ? `${stats.pendingClaims} ${stats.pendingClaims === 1 ? 'claim is' : 'claims are'} waiting for review`
                : 'All caught up'}
            </h2>
            <p className="text-sm text-ink-soft">
              {stats.pendingClaims > 0 ? 'Students are waiting for a decision.' : 'There are no claims waiting for a decision.'}
            </p>
          </div>
        </div>
        {stats.pendingClaims > 0 && <Button to="/admin/claims">Review claims</Button>}
      </section>

      <section aria-label="Numbers" className="grid grid-cols-2 gap-3 lg:grid-cols-4">
        <StatCard label="Open items" value={stats.openItems} icon={PackageOpen} to="/admin/items?status=OPEN" />
        <StatCard
          label="Claim pending"
          value={stats.claimPendingItems}
          icon={HandHelping}
          to="/admin/items?status=CLAIM_PENDING"
        />
        <StatCard label="Returned" value={stats.returnedItems} icon={PackageCheck} to="/admin/items?status=RETURNED" />
        <StatCard label="Closed" value={stats.closedItems} icon={Archive} to="/admin/items?status=CLOSED" />
        <StatCard label="Lost reports" value={stats.totalLostItems} icon={FileSearch} to="/admin/items?type=LOST" />
        <StatCard label="Found reports" value={stats.totalFoundItems} icon={CircleCheck} to="/admin/items?type=FOUND" />
        <StatCard label="Registered users" value={stats.totalUsers} icon={Users} to="/admin/users" />
        <StatCard label="Return rate" value={`${returnRate}%`} icon={Percent} />
      </section>

      <Panel title="Items by status">
        <div className="flex flex-col gap-3 px-4 py-4">
          {totalItems === 0 ? (
            <p className="text-sm text-ink-soft">No items have been reported yet.</p>
          ) : (
            <>
              <div className="flex h-3.5 gap-0.5 overflow-hidden rounded-full bg-sand" role="img" aria-label="Items by status">
                {STATUS_BARS.map(({ key, label, color }) =>
                  stats[key] > 0 ? (
                    <span
                      key={key}
                      className={color}
                      style={{ width: `${(stats[key] / totalItems) * 100}%` }}
                      title={`${label}: ${stats[key]}`}
                    />
                  ) : null,
                )}
              </div>
              <ul className="flex flex-wrap gap-x-6 gap-y-1 text-sm text-ink-soft">
                {STATUS_BARS.map(({ key, label, color }) => (
                  <li key={key} className="inline-flex items-center gap-2">
                    <span className={`size-3 rounded-full ${color}`} /> {label} <strong className="text-ink">{stats[key]}</strong>
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      </Panel>

      <div className="grid items-start gap-6 lg:grid-cols-2">
        <Panel title="Waiting for review" action={<PanelLink to="/admin/claims">All claims</PanelLink>}>
          {pending && pending.length === 0 && <p className="px-4 py-4 text-sm text-ink-soft">Nothing is waiting.</p>}
          {pending && pending.length > 0 && (
            <ul className="divide-y divide-line">
              {pending.slice(0, 5).map((claim) => (
                <li key={claim.id} className="flex items-center justify-between gap-3 px-4 py-3">
                  <div className="min-w-0">
                    <Link to="/admin/claims" className="block truncate font-medium text-ink">
                      {claim.itemName}
                    </Link>
                    <p className="truncate text-sm text-ink-soft">
                      {claim.claimantName} - {formatDate(claim.createdAt)}
                    </p>
                  </div>
                  <StatusBadge value={claim.status} />
                </li>
              ))}
            </ul>
          )}
        </Panel>

        <Panel title="Latest reports" action={<PanelLink to="/admin/items">All items</PanelLink>}>
          {allItems && latest.length === 0 && <p className="px-4 py-4 text-sm text-ink-soft">No reports yet.</p>}
          {latest.length > 0 && (
            <ul className="divide-y divide-line">
              {latest.map((item) => (
                <li key={item.id} className="flex items-center justify-between gap-3 px-4 py-3">
                  <div className="min-w-0">
                    <Link to={`/items/${item.id}`} className="block truncate font-medium text-ink">
                      {item.itemName}
                    </Link>
                    <p className="truncate text-sm text-ink-soft">
                      {item.reporterName} - {formatDate(item.createdAt)}
                    </p>
                  </div>
                  <div className="flex shrink-0 gap-2">
                    <StatusBadge value={item.type} />
                    <StatusBadge value={item.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>
    </div>
  )
}
