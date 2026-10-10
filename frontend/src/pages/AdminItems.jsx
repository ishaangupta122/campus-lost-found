import { useEffect, useMemo, useState } from 'react'
import { Link, useOutletContext, useSearchParams } from 'react-router-dom'

import Button from '../components/Button.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import ItemImage from '../components/ItemImage.jsx'
import ItemFilters, { EMPTY_FILTERS } from '../components/ItemFilters.jsx'
import PageHeader from '../components/PageHeader.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import { EmptyState, ErrorMessage, LoadingState } from '../components/States.jsx'
import { useDebounced } from '../hooks/useDebounced.js'
import { useFetch } from '../hooks/useFetch.js'
import { useNotification } from '../hooks/useNotification.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { closeItemAsAdmin, listAllItems } from '../services/adminService.js'
import { getApiError } from '../services/api.js'
import { markItemReturned } from '../services/itemService.js'
import { filterByPeriod, formatDate } from '../utils/format.js'

const PAGE_SIZE = 25

const ACTIONS = {
  close: { title: 'Close this report?', message: 'Waiting claims will be rejected.', label: 'Close report', danger: true },
  returned: {
    title: 'Mark as returned?',
    message: 'Confirm that the item was handed over to its owner.',
    label: 'Mark returned',
    danger: false,
  },
}

export default function AdminItems() {
  usePageTitle('All items')
  const notify = useNotification()
  const { stats, reloadStats } = useOutletContext()

  // The filters live in the URL (/admin/items?status=OPEN), so dashboard cards can link here
  // and a page refresh keeps them. "draft" is what is typed; "applied" is the debounced copy.
  const [searchParams, setSearchParams] = useSearchParams()
  const [draft, setDraft] = useState({ ...EMPTY_FILTERS, ...Object.fromEntries(searchParams) })
  const applied = useDebounced(draft)
  const [visible, setVisible] = useState(PAGE_SIZE)

  useEffect(() => {
    const params = Object.fromEntries(Object.entries(applied).filter(([, value]) => value))
    setSearchParams(params, { replace: true })
  }, [applied, setSearchParams])

  const { data: fetched, loading, error, reload } = useFetch(() => listAllItems(applied), [JSON.stringify(applied)])
  const items = useMemo(() => (fetched ? filterByPeriod(fetched, applied.period) : null), [fetched, applied.period])
  const [pending, setPending] = useState(null) // { action, item }
  const [working, setWorking] = useState(false)

  const changeFilters = (next) => {
    setDraft(next)
    setVisible(PAGE_SIZE)
  }

  const handleConfirm = async () => {
    setWorking(true)
    try {
      if (pending.action === 'close') await closeItemAsAdmin(pending.item.id)
      else await markItemReturned(pending.item.id)
      notify.success(pending.action === 'close' ? 'Report closed' : 'Item marked as returned')
      reload()
      reloadStats()
    } catch (err) {
      notify.error(getApiError(err).message)
    } finally {
      setWorking(false)
      setPending(null)
    }
  }

  const action = ACTIONS[pending?.action] ?? ACTIONS.close
  const total = stats ? stats.openItems + stats.claimPendingItems + stats.returnedItems + stats.closedItems : null
  const pills = [
    { value: '', label: 'All', count: total },
    { value: 'OPEN', label: 'Open', count: stats?.openItems },
    { value: 'CLAIM_PENDING', label: 'Claim pending', count: stats?.claimPendingItems },
    { value: 'RETURNED', label: 'Returned', count: stats?.returnedItems },
    { value: 'CLOSED', label: 'Closed', count: stats?.closedItems },
  ]

  return (
    <>
      <PageHeader title="All items" subtitle="Every report, whatever its status." />

      <div className="flex flex-col gap-3">
        <div className="flex flex-wrap gap-2" role="group" aria-label="Filter by status">
          {pills.map(({ value, label, count }) => (
            <button
              key={label}
              type="button"
              aria-pressed={draft.status === value}
              onClick={() => changeFilters({ ...draft, status: value })}
              className={`inline-flex cursor-pointer items-center gap-2 rounded-control border px-3 py-1 text-sm font-medium transition-colors ${
                draft.status === value
                  ? 'border-primary bg-primary-soft text-primary'
                  : 'border-line bg-card text-ink-soft hover:text-ink'
              }`}
            >
              {label}
              {count != null && <span className="text-xs opacity-80">{count}</span>}
            </button>
          ))}
        </div>

        <ItemFilters value={draft} onChange={changeFilters} allStatusesLabel="Any status" />

        {loading && !items && <LoadingState message="Loading items..." />}
        {error && <ErrorMessage message={error} onRetry={reload} />}
        {items && items.length === 0 && <EmptyState title="No items found" message="Try different filters." />}

        {items && items.length > 0 && (
          <>
            <p className="text-sm text-ink-soft">
              {items.length} {items.length === 1 ? 'item' : 'items'}
            </p>
            <div className={`overflow-x-auto rounded-card bg-card shadow-card ${loading ? 'opacity-60' : ''}`}>
              <table className="w-full text-left text-sm">
                <thead className="border-b border-line bg-sand text-xs tracking-wide text-ink-soft uppercase">
                  <tr>
                    <th className="px-3 py-2 font-medium">Item</th>
                    <th className="hidden px-3 py-2 font-medium sm:table-cell">Status</th>
                    <th className="hidden px-3 py-2 font-medium md:table-cell">Reported by</th>
                    <th className="hidden px-3 py-2 font-medium lg:table-cell">Location</th>
                    <th className="hidden px-3 py-2 font-medium md:table-cell">Date</th>
                    <th className="px-3 py-2 text-right font-medium">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-line">
                  {items.slice(0, visible).map((item) => {
                    const active = item.status === 'OPEN' || item.status === 'CLAIM_PENDING'
                    return (
                      <tr key={item.id} className="align-middle hover:bg-sand/50">
                        <td className="px-3 py-2">
                          <div className="flex items-center gap-3">
                            <ItemImage imagePath={item.imagePath} className="size-10 shrink-0 rounded-md bg-sand object-cover" />
                            <div className="min-w-0">
                              <Link to={`/items/${item.id}`} className="block truncate font-medium text-ink">
                                {item.itemName}
                              </Link>
                              <p className="mt-0.5 flex items-center gap-2 text-xs text-ink-soft">
                                <StatusBadge value={item.type} /> {item.category}
                              </p>
                              <div className="mt-1 sm:hidden">
                                <StatusBadge value={item.status} />
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="hidden px-3 py-2 sm:table-cell">
                          <StatusBadge value={item.status} />
                        </td>
                        <td className="hidden px-3 py-2 text-ink-soft md:table-cell">{item.reporterName}</td>
                        <td className="hidden px-3 py-2 text-ink-soft lg:table-cell">{item.location}</td>
                        <td className="hidden px-3 py-2 whitespace-nowrap text-ink-soft md:table-cell">
                          {formatDate(item.reportDate)}
                        </td>
                        <td className="px-3 py-2">
                          <div className="flex flex-wrap justify-end gap-2">
                            {item.status === 'CLAIM_PENDING' && (
                              <Button size="sm" onClick={() => setPending({ action: 'returned', item })}>
                                Mark returned
                              </Button>
                            )}
                            {active && (
                              <Button size="sm" variant="secondary" onClick={() => setPending({ action: 'close', item })}>
                                Close
                              </Button>
                            )}
                            {!active && <span className="text-xs text-ink-soft">No actions</span>}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
            {items.length > visible && (
              <div className="flex justify-center">
                <Button variant="secondary" onClick={() => setVisible(visible + PAGE_SIZE)}>
                  Load more
                </Button>
              </div>
            )}
          </>
        )}
      </div>

      <ConfirmDialog
        open={pending !== null}
        title={action.title}
        message={action.message}
        confirmLabel={action.label}
        danger={action.danger}
        loading={working}
        onConfirm={handleConfirm}
        onCancel={() => !working && setPending(null)}
      />
    </>
  )
}
