import { Link, useOutletContext } from 'react-router-dom'
import { Archive, ArrowRight, CircleCheck, Clock, FileSearch, HandHelping, PackageCheck, PackageOpen, Users } from 'lucide-react'

import Button from '../components/Button.jsx'
import ClaimCard from '../components/ClaimCard.jsx'
import PageHeader from '../components/PageHeader.jsx'
import StatCard from '../components/StatCard.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { ErrorMessage, LoadingState } from '../components/States.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { getPendingClaims, listAllItems } from '../services/adminService.js'
import { formatDate } from '../utils/format.js'

// Segments of the "items by status" bar. (Full class names so Tailwind finds them.)
const STATUS_BARS = [
  { key: 'openItems', label: 'Open', bar: 'bg-ok-fg', dot: 'bg-ok-fg' },
  { key: 'claimPendingItems', label: 'Claim pending', bar: 'bg-warn-fg', dot: 'bg-warn-fg' },
  { key: 'returnedItems', label: 'Returned', bar: 'bg-info-fg', dot: 'bg-info-fg' },
  { key: 'closedItems', label: 'Closed', bar: 'bg-stone-fg', dot: 'bg-stone-fg' },
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
    <div className="flex flex-col gap-4">
      <PageHeader title="Admin dashboard" subtitle="A quick look at what is happening on campus." />

      {/* What needs the admin's attention right now */}
      <section
        className={`flex flex-wrap items-center justify-between gap-3 rounded-card border p-4 ${
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
            <h2 className="text-lg">
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

      <section aria-label="Numbers" className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
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
        <StatCard label="Return rate" value={`${returnRate}%`} icon={PackageCheck} />
      </section>

      <section className="flex flex-col gap-2 rounded-card bg-card shadow-card p-4">
        <h2 className="text-lg">Items by status</h2>
        {totalItems === 0 ? (
          <p className="text-sm text-ink-soft">No items have been reported yet.</p>
        ) : (
          <>
            <div className="flex h-3 overflow-hidden rounded-full bg-sand" role="img" aria-label="Items by status">
              {STATUS_BARS.map(({ key, label, bar }) =>
                stats[key] > 0 ? (
                  <span
                    key={key}
                    className={bar}
                    style={{ width: `${(stats[key] / totalItems) * 100}%` }}
                    title={`${label}: ${stats[key]}`}
                  />
                ) : null,
              )}
            </div>
            <ul className="flex flex-wrap gap-x-5 gap-y-1 text-sm text-ink-soft">
              {STATUS_BARS.map(({ key, label, dot }) => (
                <li key={key} className="inline-flex items-center gap-2">
                  <span className={`size-2.5 rounded-full ${dot}`} /> {label} <strong className="text-ink">{stats[key]}</strong>
                </li>
              ))}
            </ul>
          </>
        )}
      </section>

      <div className="grid gap-4 lg:grid-cols-2">
        <section className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <h2 className="text-lg">Waiting for review</h2>
            <Link to="/admin/claims" className="inline-flex items-center gap-1 text-sm">
              All claims <ArrowRight size={14} aria-hidden="true" />
            </Link>
          </div>
          {pending && pending.length === 0 && (
            <p className="rounded-card bg-card shadow-card p-4 text-sm text-ink-soft">Nothing is waiting.</p>
          )}
          {pending?.slice(0, 3).map((claim) => (
            <ClaimCard key={claim.id} claim={claim} showClaimant />
          ))}
        </section>

        <section className="flex flex-col gap-2">
          <div className="flex items-center justify-between">
            <h2 className="text-lg">Latest reports</h2>
            <Link to="/admin/items" className="inline-flex items-center gap-1 text-sm">
              All items <ArrowRight size={14} aria-hidden="true" />
            </Link>
          </div>
          {allItems && latest.length === 0 && (
            <p className="rounded-card bg-card shadow-card p-4 text-sm text-ink-soft">No reports yet.</p>
          )}
          {latest.length > 0 && (
            <ul className="divide-y divide-line rounded-card bg-card shadow-card">
              {latest.map((item) => (
                <li key={item.id} className="flex flex-wrap items-center justify-between gap-2 px-3 py-2">
                  <div className="min-w-0">
                    <Link to={`/items/${item.id}`} className="block truncate font-medium text-ink">
                      {item.itemName}
                    </Link>
                    <p className="text-xs text-ink-soft">
                      {item.reporterName} - {formatDate(item.createdAt)}
                    </p>
                  </div>
                  <div className="flex gap-2">
                    <StatusBadge value={item.type} />
                    <StatusBadge value={item.status} />
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </div>
  )
}
