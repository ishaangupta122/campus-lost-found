import { useEffect, useMemo, useState } from 'react'
import { Link, useOutletContext, useSearchParams } from 'react-router-dom'
import { PackageCheck, X } from 'lucide-react'

import Button from '../components/Button.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import ItemImage from '../components/ItemImage.jsx'
import ItemFilters, { EMPTY_FILTERS } from '../components/ItemFilters.jsx'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
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
              className={`inline-flex cursor-pointer items-center gap-2 rounded-md border px-3 py-1.5 text-sm font-medium transition-colors ${
                draft.status === value
                  ? 'border-primary bg-primary-soft text-primary'
                  : 'border-line bg-card text-ink-soft hover:text-ink'
              }`}
            >
              {label}
              {count != null && (
                <span className="rounded-full bg-sand px-1.5 text-xs font-semibold text-ink-soft">{count}</span>
              )}
            </button>
          ))}
        </div>

        <ItemFilters value={draft} onChange={changeFilters} allStatusesLabel="Any status" hideStatus defaultOpen={false} />

        {loading && !items && <LoadingState message="Loading items..." />}
        {error && <ErrorMessage message={error} onRetry={reload} />}
        {items && items.length === 0 && <EmptyState title="No items found" message="Try different filters." />}

        {items && items.length > 0 && (
          <>
            <Panel title={`${items.length} ${items.length === 1 ? 'item' : 'items'}`}>
              <div className={`overflow-x-auto ${loading ? 'opacity-60' : ''}`}>
                <table className="w-full text-left text-sm">
                  <thead className="border-b border-line text-sm whitespace-nowrap text-ink-soft">
                    <tr>
                      <th className="px-4 py-2.5 font-medium">Item</th>
                      <th className="hidden px-3 py-2.5 font-medium sm:table-cell">Status</th>
                      <th className="hidden px-3 py-2.5 font-medium md:table-cell">Reported by</th>
                      <th className="hidden px-3 py-2.5 font-medium lg:table-cell">Location</th>
                      <th className="hidden px-4 py-2.5 text-right font-medium sm:table-cell">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-line">
                    {items.slice(0, visible).map((item) => {
                      const active = item.status === 'OPEN' || item.status === 'CLAIM_PENDING'
                      return (
                        <tr key={item.id} className="align-middle hover:bg-sand/50">
                          <td className="px-4 py-3">
                            <div className="flex items-center gap-3">
                              <ItemImage imagePath={item.imagePath} className="size-10 shrink-0 rounded-md bg-sand object-cover" />
                              <div className="max-w-64 min-w-0">
                                <Link to={`/items/${item.id}`} className="block truncate font-medium text-ink">
                                  {item.itemName}
                                </Link>
                                <p className="mt-0.5 flex items-center gap-2 text-sm text-ink-soft">
                                  <StatusBadge value={item.type} /> {item.category}
                                </p>
                                <div className="mt-1 sm:hidden">
                                  <StatusBadge value={item.status} />
                                </div>
                                <div className="mt-2 flex flex-wrap gap-2 sm:hidden">
                              {item.status === 'CLAIM_PENDING' && (
                                <Button size="sm" onClick={() => setPending({ action: 'returned', item })}>
                                  <PackageCheck size={14} aria-hidden="true" /> Mark returned
                                </Button>
                              )}
                              {active && (
                                <Button size="sm" variant="soft" onClick={() => setPending({ action: 'close', item })}>
                                  <X size={14} aria-hidden="true" /> Close
                                </Button>
                              )}
                              {!active && <span className="text-sm text-ink-soft">No actions</span>}
                            </div>
                              </div>
                            </div>
                          </td>
                          <td className="hidden px-3 py-3 sm:table-cell">
                            <StatusBadge value={item.status} />
                          </td>
                          <td className="hidden px-3 py-3 whitespace-nowrap md:table-cell">
                            <p className="max-w-36 truncate text-ink" title={item.reporterName}>
                              {item.reporterName}
                            </p>
                            <p className="text-sm text-ink-soft">{formatDate(item.reportDate)}</p>
                          </td>
                          <td className="hidden max-w-52 truncate px-3 py-3 whitespace-nowrap text-ink-soft lg:table-cell" title={item.location}>
                            {item.location}
                          </td>
                          <td className="hidden px-4 py-3 sm:table-cell">
                            <div className="flex justify-end gap-2">
                              {item.status === 'CLAIM_PENDING' && (
                                <Button size="sm" onClick={() => setPending({ action: 'returned', item })}>
                                  <PackageCheck size={14} aria-hidden="true" /> Mark returned
                                </Button>
                              )}
                              {active && (
                                <Button size="sm" variant="soft" onClick={() => setPending({ action: 'close', item })}>
                                  <X size={14} aria-hidden="true" /> Close
                                </Button>
                              )}
                              {!active && <span className="text-sm text-ink-soft">No actions</span>}
                            </div>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </Panel>
            {items.length > visible && (
              <div className="flex justify-center pt-2 pb-4">
                <Button variant="secondary" className="px-8" onClick={() => setVisible(visible + PAGE_SIZE)}>
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
