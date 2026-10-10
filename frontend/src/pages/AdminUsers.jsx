import { useMemo, useState } from 'react'
import { Search } from 'lucide-react'

import PageHeader from '../components/PageHeader.jsx'
import SegmentedControl from '../components/SegmentedControl.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { EmptyState, ErrorMessage, LoadingState } from '../components/States.jsx'
import Button from '../components/Button.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { listUsers } from '../services/adminService.js'
import { formatDate, formatLabel } from '../utils/format.js'

const PAGE_SIZE = 25

const ROLE_OPTIONS = [
  { value: '', label: 'All' },
  { value: 'STUDENT', label: 'Students' },
  { value: 'ADMIN', label: 'Admins' },
]

export default function AdminUsers() {
  usePageTitle('Users')
  const { data: users, loading, error, reload } = useFetch(() => listUsers(), [])
  const [search, setSearch] = useState('')
  const [role, setRole] = useState('')
  const [visible, setVisible] = useState(PAGE_SIZE)

  // Search and role filter run in the browser: the whole list is already loaded.
  const filtered = useMemo(() => {
    const text = search.trim().toLowerCase()
    return (users ?? []).filter(
      (user) =>
        (!role || user.role === role) &&
        (!text || user.name.toLowerCase().includes(text) || user.email.toLowerCase().includes(text)),
    )
  }, [users, search, role])

  const count = (value) => (users ?? []).filter((user) => user.role === value).length

  return (
    <>
      <PageHeader
        title="Users"
        subtitle={users ? `${users.length} registered (${count('STUDENT')} students, ${count('ADMIN')} admins)` : 'Everyone who has an account.'}
      />

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap items-center gap-3 rounded-card bg-card p-3 shadow-card" role="search">
          <div className="relative min-w-56 flex-1">
            <Search
              size={18}
              className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-ink-soft"
              aria-hidden="true"
            />
            <input
              type="search"
              aria-label="Search users"
              placeholder="Search by name or email"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
                setVisible(PAGE_SIZE)
              }}
              className="w-full rounded-control border border-line bg-card py-2 pr-3 pl-10 text-base placeholder:text-ink-soft/70 hover:border-ink-soft focus:border-primary focus-visible:outline-offset-0"
            />
          </div>
          <SegmentedControl
            label="Role"
            options={ROLE_OPTIONS}
            value={role}
            onChange={(next) => {
              setRole(next)
              setVisible(PAGE_SIZE)
            }}
          />
        </div>

        {loading && !users && <LoadingState message="Loading users..." />}
        {error && <ErrorMessage message={error} onRetry={reload} />}
        {users && filtered.length === 0 && <EmptyState title="No users found" message="Try a different search." />}

        {users && filtered.length > 0 && (
          <>
            <p className="text-sm text-ink-soft" aria-live="polite">
              Showing {Math.min(visible, filtered.length)} of {filtered.length} {filtered.length === 1 ? 'user' : 'users'}
            </p>
            <div className="overflow-x-auto rounded-card bg-card shadow-card">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-line bg-sand text-xs tracking-wide text-ink-soft uppercase">
                  <tr>
                    <th className="px-3 py-2 font-medium">Name</th>
                    <th className="px-3 py-2 font-medium">Email</th>
                    <th className="px-3 py-2 font-medium">Role</th>
                    <th className="hidden px-3 py-2 font-medium sm:table-cell">Sign-in</th>
                    <th className="hidden px-3 py-2 font-medium md:table-cell">Joined</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {filtered.slice(0, visible).map((user) => (
                    <tr key={user.id} className="hover:bg-sand/50">
                      <td className="px-3 py-2 font-medium">{user.name}</td>
                      <td className="px-3 py-2 break-all text-ink-soft">{user.email}</td>
                      <td className="px-3 py-2">
                        <StatusBadge value={user.role === 'ADMIN' ? 'REJECTED' : 'FOUND'} label={formatLabel(user.role)} />
                      </td>
                      <td className="hidden px-3 py-2 text-ink-soft sm:table-cell">
                        {user.googleAccount ? 'Google' : 'Password'}
                      </td>
                      <td className="hidden px-3 py-2 whitespace-nowrap text-ink-soft md:table-cell">
                        {formatDate(user.createdAt)}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {filtered.length > visible && (
              <div className="flex justify-center">
                <Button variant="secondary" onClick={() => setVisible(visible + PAGE_SIZE)}>
                  Load more
                </Button>
              </div>
            )}
          </>
        )}
      </div>
    </>
  )
}
