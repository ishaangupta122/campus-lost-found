import { Link, useNavigate } from 'react-router-dom'
import { LogOut } from 'lucide-react'

import Button from '../components/Button.jsx'
import PageHeader from '../components/PageHeader.jsx'
import Panel, { PanelLink } from '../components/Panel.jsx'
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

// A short list of the latest items or claims; rows are { id, to, name, date, status }.
function RecentList({ title, seeAllTo, rows, empty }) {
  return (
    <Panel title={title} action={<PanelLink to={seeAllTo}>See all</PanelLink>}>
      {rows.length === 0 ? (
        <p className="px-4 py-4 text-sm text-ink-soft">{empty}</p>
      ) : (
        <ul className="divide-y divide-line">
          {rows.slice(0, RECENT).map((row) => (
            <li key={row.id} className="flex items-center justify-between gap-3 px-4 py-3">
              <div className="min-w-0">
                <Link to={row.to} className="block truncate font-medium text-ink">
                  {row.name}
                </Link>
                <p className="text-sm text-ink-soft">{formatDate(row.date)}</p>
              </div>
              <StatusBadge value={row.status} />
            </li>
          ))}
        </ul>
      )}
    </Panel>
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
        <RecentList
          title="Recent reports"
          seeAllTo="/my-items"
          rows={reports.map((item) => ({
            id: item.id,
            to: `/items/${item.id}`,
            name: item.itemName,
            date: item.reportDate,
            status: item.status,
          }))}
          empty={
            <>
              No reports yet. <Link to="/report">Report an item</Link>
            </>
          }
        />
        <RecentList
          title="Recent claims"
          seeAllTo="/my-claims"
          rows={myClaims.map((claim) => ({
            id: claim.id,
            to: `/items/${claim.itemId}`,
            name: claim.itemName,
            date: claim.createdAt,
            status: claim.status,
          }))}
          empty={
            <>
              No claims yet. <Link to="/">Browse items</Link>
            </>
          }
        />
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
    <div className="flex flex-col gap-6">
      <PageHeader title="Profile" subtitle="Your account and recent activity.">
        <Button variant="secondary" onClick={handleLogout}>
          <LogOut size={16} aria-hidden="true" /> Log out
        </Button>
      </PageHeader>

      <Panel title="Account" action={<StatusBadge value={isAdmin ? 'REJECTED' : 'FOUND'} label={isAdmin ? 'Administrator' : 'Student'} />}>
        <dl className="flex flex-col gap-3 px-4 py-4">
          <div className="grid gap-1 sm:grid-cols-[9rem_1fr] sm:gap-4">
            <dt className="text-sm text-ink-soft">Name</dt>
            <dd className="text-sm text-ink">{user.name}</dd>
          </div>
          <div className="grid gap-1 sm:grid-cols-[9rem_1fr] sm:gap-4">
            <dt className="text-sm text-ink-soft">Email</dt>
            <dd className="text-sm text-ink break-all">{user.email}</dd>
          </div>
          <div className="grid gap-1 sm:grid-cols-[9rem_1fr] sm:gap-4">
            <dt className="text-sm text-ink-soft">Role</dt>
            <dd className="text-sm text-ink">{isAdmin ? 'Administrator: reviews claims and manages every report' : 'Student: reports items and claims found ones'}</dd>
          </div>
        </dl>
      </Panel>

      {isStudent && <StudentActivity />}

      {isAdmin && (
        <Panel title="Admin area">
          <div className="flex flex-wrap items-center justify-between gap-3 px-4 py-4">
            <p className="text-sm text-ink-soft">Review claims and manage every report on campus.</p>
            <Button to="/admin">Open dashboard</Button>
          </div>
        </Panel>
      )}
    </div>
  )
}
