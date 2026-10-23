import { Fragment, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowDown, ArrowRight, Plus } from 'lucide-react'

import Button from '../components/Button.jsx'
import ItemCard from '../components/ItemCard.jsx'
import ItemTable from '../components/ItemTable.jsx'
import SegmentedControl from '../components/SegmentedControl.jsx'
import ItemFilters, { EMPTY_FILTERS } from '../components/ItemFilters.jsx'
import { ItemGridSkeleton } from '../components/Skeletons.jsx'
import { EmptyState, ErrorMessage } from '../components/States.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useDebounced } from '../hooks/useDebounced.js'
import { useFetch } from '../hooks/useFetch.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { listItems } from '../services/itemService.js'
import { filterByPeriod } from '../utils/format.js'

// How many items are shown at first and added by each "Load more" click.
const PAGE_SIZE = { cards: 9, table: 20 }
const VIEW_KEY = 'lf_items_view'

const VIEWS = [
  { value: 'cards', label: 'Cards' },
  { value: 'table', label: 'Table' },
]

// The chosen view is remembered in this browser; storage can be blocked, so never rely on it.
function readView() {
  try {
    return localStorage.getItem(VIEW_KEY) === 'table' ? 'table' : 'cards'
  } catch {
    return 'cards'
  }
}

// The public list shows open items; "Returned" is the only other choice offered here.
const STATUS_OPTIONS = [{ value: 'RETURNED', label: 'Returned' }]

const STEPS = [
  { title: 'Report', text: 'Post what you lost or found.' },
  { title: 'Claim', text: 'Name a detail only you know.' },
  { title: 'Collect', text: 'Admin approves, you collect.' },
]

export default function Home() {
  usePageTitle('')
  const { isAuthenticated, isAdmin } = useAuth()
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [storedView, setView] = useState(readView)
  // Only admins get the table view; everyone else always sees cards.
  const view = isAdmin ? storedView : 'cards'
  const [visible, setVisible] = useState(PAGE_SIZE[view])

  // Search after the user stops typing; the list reloads whenever the filters change.
  const applied = useDebounced(filters)
  const { data: fetched, loading, error, reload } = useFetch(() => listItems(applied), [JSON.stringify(applied)])
  // The time filter is applied here, on the report date of the loaded items.
  const items = useMemo(() => (fetched ? filterByPeriod(fetched, applied.period) : null), [fetched, applied.period])

  const pageSize = PAGE_SIZE[view]

  const changeFilters = (next) => {
    setFilters(next)
    setVisible(pageSize)
  }

  const changeView = (next) => {
    setView(next)
    setVisible(PAGE_SIZE[next])
    try {
      localStorage.setItem(VIEW_KEY, next)
    } catch {
      /* the choice just is not remembered */
    }
  }

  return (
    <div className="flex flex-col gap-6">
      <section className="flex flex-col gap-4 rounded-md bg-primary p-5 text-on-primary sm:flex-row sm:items-center sm:justify-between sm:p-6">
        <div>
          <h1 className="text-on-primary">Lost or found something on campus?</h1>
          <p className="mt-1 text-sm text-on-primary/80">Report it here, or search the list and claim what is yours.</p>
        </div>
        {!isAdmin && (
          <Link
            to="/report"
            className="inline-flex shrink-0 items-center justify-center gap-2 rounded-control bg-on-primary px-4 py-2 text-sm font-medium whitespace-nowrap text-primary no-underline transition-colors hover:bg-primary-soft hover:no-underline"
          >
            <Plus size={16} aria-hidden="true" /> Report an item
          </Link>
        )}
      </section>

      {!isAuthenticated && (
        <ol
          aria-label="How it works"
          className="flex flex-col gap-3 rounded-md border border-line bg-card px-5 py-4 lg:flex-row lg:items-center lg:justify-between"
        >
          {STEPS.map(({ title, text }, index) => (
            <Fragment key={title}>
              <li className="flex items-center gap-3">
                <span className="inline-flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-soft text-sm font-semibold text-primary">
                  {index + 1}
                </span>
                <div>
                  <p className="text-sm leading-5 font-semibold text-ink">{title}</p>
                  <p className="text-sm leading-5 text-ink-soft">{text}</p>
                </div>
              </li>
              {index < STEPS.length - 1 && (
                <li aria-hidden="true" className="flex shrink-0 justify-start pl-1.5 text-primary lg:p-0">
                  <ArrowRight size={22} className="hidden lg:block" />
                  <ArrowDown size={22} className="lg:hidden" />
                </li>
              )}
            </Fragment>
          ))}
        </ol>
      )}

      <section id="items" className="flex flex-col gap-4">
        <ItemFilters
          value={filters}
          onChange={changeFilters}
          allStatusesLabel="Open and claim pending"
          statusOptions={STATUS_OPTIONS}
          defaultOpen={false}
        />

        {error && <ErrorMessage message={error} onRetry={reload} />}
        {loading && !items && <ItemGridSkeleton />}
        {items && items.length === 0 && (
          <EmptyState
            title="No items found"
            message="Try a different search, or report the item yourself so others can find it."
            action={isAdmin ? undefined : { label: 'Report an item', to: '/report' }}
          />
        )}
        {items && items.length > 0 && (
          <>
            <div className="flex flex-wrap items-center justify-between gap-3">
              <p className="text-sm text-ink-soft" aria-live="polite">
                Showing {Math.min(visible, items.length)} of {items.length} {items.length === 1 ? 'item' : 'items'}
              </p>
              {isAdmin && <SegmentedControl label="View" options={VIEWS} value={view} onChange={changeView} />}
            </div>

            {view === 'table' ? (
              <ItemTable items={items.slice(0, visible)} dimmed={loading} />
            ) : (
              <div className={`grid gap-4 sm:grid-cols-2 lg:grid-cols-3 ${loading ? 'opacity-60' : ''}`}>
                {items.slice(0, visible).map((item) => (
                  <ItemCard key={item.id} item={item} />
                ))}
              </div>
            )}

            {items.length > visible && (
              <div className="flex justify-center pt-4 pb-6">
                <Button variant="secondary" className="px-8" onClick={() => setVisible(visible + pageSize)}>
                  Load more
                </Button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  )
}
