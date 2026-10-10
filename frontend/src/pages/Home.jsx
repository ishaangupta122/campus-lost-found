import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'

import Button from '../components/Button.jsx'
import ItemCard from '../components/ItemCard.jsx'
import SegmentedControl from '../components/SegmentedControl.jsx'
import ItemFilters, { EMPTY_FILTERS } from '../components/ItemFilters.jsx'
import { ItemGridSkeleton } from '../components/Skeletons.jsx'
import { EmptyState, ErrorMessage } from '../components/States.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useDebounced } from '../hooks/useDebounced.js'
import { useFetch } from '../hooks/useFetch.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { listItems } from '../services/itemService.js'

const PAGE_SIZE = 12

const TABS = [
  { value: '', label: 'Available items' },
  { value: 'RETURNED', label: 'Returned items' },
]

const STEPS = [
  { title: 'Report', text: 'Post what you lost or found.' },
  { title: 'Match', text: 'Claim it with a detail only you know.' },
  { title: 'Return', text: 'An admin approves and you collect it.' },
]

export default function Home() {
  usePageTitle('')
  const { isAuthenticated, isAdmin } = useAuth()
  const [filters, setFilters] = useState(EMPTY_FILTERS)
  const [visible, setVisible] = useState(PAGE_SIZE)

  // Search after the user stops typing; the list reloads whenever the filters change.
  const applied = useDebounced(filters)
  const { data: items, loading, error, reload } = useFetch(() => listItems(applied), [JSON.stringify(applied)])

  const changeFilters = (next) => {
    setFilters(next)
    setVisible(PAGE_SIZE)
  }

  return (
    <div className="flex flex-col gap-10">
      <section className="flex flex-col gap-4 rounded-card bg-primary p-5 text-on-primary sm:flex-row sm:items-center sm:justify-between sm:p-6">
        <div>
          <h1 className="text-on-primary">Lost something? Let&apos;s find it.</h1>
          <p className="mt-1 text-sm text-on-primary/80">Browse what has been found on campus and claim what is yours.</p>
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
        <ol aria-label="How it works" className="grid gap-4 sm:grid-cols-3 sm:gap-6">
          {STEPS.map(({ title, text }, index) => (
            <li key={title} className="flex items-center gap-3">
              <span className="inline-flex size-8 shrink-0 items-center justify-center rounded-full bg-primary-soft text-sm font-semibold text-primary">
                {index + 1}
              </span>
              <div className="leading-snug">
                <h2 className="text-sm">{title}</h2>
                <p className="text-sm text-ink-soft">{text}</p>
              </div>
            </li>
          ))}
        </ol>
      )}

      <section id="items" className="flex flex-col gap-4">
                <ItemFilters value={filters} onChange={changeFilters} allStatusesLabel="Open and claim pending"
          hideStatus
          leading={
            <SegmentedControl
              label="Show"
              options={TABS}
              value={filters.status}
              onChange={(status) => changeFilters({ ...filters, status })}
            />
          }
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
            <p className="text-sm text-ink-soft" aria-live="polite">
              {items.length} {items.length === 1 ? 'item' : 'items'}
            </p>
            <div className={`grid gap-4 sm:grid-cols-2 lg:grid-cols-3 ${loading ? 'opacity-60' : ''}`}>
              {items.slice(0, visible).map((item) => (
                <ItemCard key={item.id} item={item} />
              ))}
            </div>
            {items.length > visible && (
              <div className="flex justify-center">
                <Button variant="secondary" onClick={() => setVisible(visible + PAGE_SIZE)}>
                  Show more
                </Button>
              </div>
            )}
          </>
        )}
      </section>
    </div>
  )
}
