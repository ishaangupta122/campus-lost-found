import { Link, useNavigate } from 'react-router-dom'
import { ArrowRight, LogOut, Mail, ShieldCheck } from 'lucide-react'

import Button from '../components/Button.jsx'
import StatCard from '../components/StatCard.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { ErrorMessage, LoadingState } from '../components/States.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useFetch } from '../hooks/useFetch.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { getMyClaims } from '../services/claimService.js'
import { getMyItems } from '../services/itemService.js'
import { formatDate } from '../utils/format.js'

const RECENT = 4

// A small heading with a "see all" link, used above each recent list.
function ListHeader({ title, to }) {
  return (
    <div className="flex items-center justify-between">
      <h2>{title}</h2>
      <Link to={to} className="inline-flex items-center gap-1 text-sm">
        See all <ArrowRight size={14} aria-hidden="true" />
      </Link>
    </div>
  )
}

// The student's own numbers and latest activity. Only students have reports and claims.
function StudentActivity() {
  const items = useFetch(() => getMyItems(), [])
  const claims = useFetch(() => getMyClaims(), [])

  if (items.error || claims.error) {
    return <ErrorMessage message={items.error || claims.error} onRetry={() => (items.reload(), claims.reload())} />
  }
  if (!items.data || !claims.data) return <LoadingState message="Loading your activity..." />

  const reports = items.data
  const myClaims = claims.data
  const count = (list, test) => list.filter(test).length

  return (
    <>
      <section className="grid grid-cols-2 gap-3 sm:grid-cols-4" aria-label="Your numbers">
        <StatCard label="Reports" value={reports.length} to="/my-items" />
        <StatCard label="Still open" value={count(reports, (i) => i.status === 'OPEN' || i.status === 'CLAIM_PENDING')} to="/my-items" />
        <StatCard label="Returned" value={count(reports, (i) => i.status === 'RETURNED')} to="/my-items" />
        <StatCard label="Claims" value={myClaims.length} to="/my-claims" />
      </section>

      <div className="grid gap-6 md:grid-cols-2">
        <section className="flex flex-col gap-3">
          <ListHeader title="Recent reports" to="/my-items" />
          {reports.length === 0 ? (
            <p className="rounded-card bg-card p-4 text-sm text-ink-soft shadow-card">
              No reports yet. <Link to="/report">Report an item</Link>
            </p>
          ) : (
            <ul className="divide-y divide-line rounded-card bg-card shadow-card">
              {reports.slice(0, RECENT).map((item) => (
                <li key={item.id} className="flex items-center justify-between gap-3 px-4 py-2.5">
                  <div className="min-w-0">
                    <Link to={`/items/${item.id}`} className="block truncate font-medium text-ink">
                      {item.itemName}
                    </Link>
                    <p className="text-xs text-ink-soft">{formatDate(item.reportDate)}</p>
                  </div>
                  <StatusBadge value={item.status} />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className="flex flex-col gap-3">
          <ListHeader title="Recent claims" to="/my-claims" />
          {myClaims.length === 0 ? (
            <p className="rounded-card bg-card p-4 text-sm text-ink-soft shadow-card">
              No claims yet. <Link to="/">Browse items</Link>
            </p>
          ) : (
            <ul className="divide-y divide-line rounded-card bg-card shadow-card">
              {myClaims.slice(0, RECENT).map((claim) => (
                <li key={claim.id} className="flex items-center justify-between gap-3 px-4 py-2.5">
                  <div className="min-w-0">
                    <Link to={`/items/${claim.itemId}`} className="block truncate font-medium text-ink">
                      {claim.itemName}
                    </Link>
                    <p className="text-xs text-ink-soft">{formatDate(claim.createdAt)}</p>
                  </div>
                  <StatusBadge value={claim.status} />
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </>
  )
}

export default function Profile() {
  usePageTitle('Profile')
  const { user, isAdmin, isStudent, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-6">
      <section className="flex flex-wrap items-center gap-4 rounded-card bg-card p-5 shadow-card">
        <span
          aria-hidden="true"
          className="inline-flex size-16 shrink-0 items-center justify-center rounded-full bg-primary font-display text-2xl font-semibold text-on-primary"
        >
          {user.name.trim().charAt(0).toUpperCase()}
        </span>

        <div className="min-w-0 flex-1">
          <h1 className="truncate">{user.name}</h1>
          <p className="mt-0.5 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-ink-soft">
            <span className="inline-flex items-center gap-1.5">
              <Mail size={14} aria-hidden="true" /> {user.email}
            </span>
            <span className="inline-flex items-center gap-1.5">
              <ShieldCheck size={14} aria-hidden="true" /> {isAdmin ? 'Administrator' : 'Student'}
            </span>
          </p>
        </div>

        <Button variant="secondary" onClick={handleLogout}>
          <LogOut size={16} aria-hidden="true" /> Log out
        </Button>
      </section>

      {isStudent && <StudentActivity />}

      {isAdmin && (
        <section className="flex flex-wrap items-center justify-between gap-3 rounded-card bg-card p-5 shadow-card">
          <div>
            <h2>Admin area</h2>
            <p className="text-sm text-ink-soft">Review claims and manage every report on campus.</p>
          </div>
          <Button to="/admin">Open dashboard</Button>
        </section>
      )}
    </div>
  )
}
