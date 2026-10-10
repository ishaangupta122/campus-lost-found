import { NavLink, Outlet } from 'react-router-dom'

import { useFetch } from '../hooks/useFetch.js'
import { getDashboard } from '../services/adminService.js'

const TAB =
  'inline-flex items-center gap-2 rounded-control px-3 py-1.5 text-sm font-medium text-ink-soft hover:bg-sand hover:text-ink hover:no-underline'
const tabClass = ({ isActive }) =>
  `${TAB} ${isActive ? 'bg-primary-soft text-primary hover:bg-primary-soft hover:text-primary' : ''}`

/**
 * Shared by the admin pages: a tab bar on top. It also loads the dashboard numbers once
 * (for the "pending claims" count on the tab) and hands them to the pages below through the
 * Outlet context, together with a reload function to call after an admin changes something.
 */
export default function AdminLayout() {
  const { data: stats, error, reload } = useFetch(() => getDashboard(), [])

  return (
    <div>
      <nav aria-label="Admin" className="mb-4 flex flex-wrap gap-1">
        <NavLink to="/admin" end className={tabClass}>
          Dashboard
        </NavLink>
        <NavLink to="/admin/claims" className={tabClass}>
          Pending claims
          {stats?.pendingClaims > 0 && (
            <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-primary px-1.5 text-xs text-on-primary">
              {stats.pendingClaims}
            </span>
          )}
        </NavLink>
        <NavLink to="/admin/items" className={tabClass}>
          All items
        </NavLink>
        <NavLink to="/admin/users" className={tabClass}>
          Users
          {stats?.totalUsers > 0 && (
            <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-sand px-1.5 text-xs text-ink-soft">
              {stats.totalUsers}
            </span>
          )}
        </NavLink>
      </nav>
      <Outlet context={{ stats, statsError: error, reloadStats: reload }} />
    </div>
  )
}
