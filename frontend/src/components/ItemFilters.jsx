import { useState } from 'react'
import { Search, SlidersHorizontal, X } from 'lucide-react'

import Button from './Button.jsx'
import Input from './Input.jsx'
import SegmentedControl from './SegmentedControl.jsx'
import Select from './Select.jsx'
import { CATEGORIES, PERIOD_OPTIONS } from '../utils/constants.js'

export const EMPTY_FILTERS = { q: '', type: '', category: '', location: '', status: '', period: '' }

const STATUS_OPTIONS = [
  { value: 'OPEN', label: 'Open' },
  { value: 'CLAIM_PENDING', label: 'Claim pending' },
  { value: 'RETURNED', label: 'Returned' },
  { value: 'CLOSED', label: 'Closed' },
]

const TYPE_OPTIONS = [
  { value: '', label: 'All' },
  { value: 'LOST', label: 'Lost' },
  { value: 'FOUND', label: 'Found' },
]

/**
 * Search box + Lost/Found switch, with the other filters (category, location, time, status)
 * in a collapsible "Filters" panel. Search runs by itself as the user types. The parent owns the values.
 * allStatusesLabel: what the empty status choice means ("Open and claim pending" for the public list).
 * statusOptions: replaces the default status choices; defaultOpen: whether the panel starts open.
 */
export default function ItemFilters({ value, onChange, allStatusesLabel, statusOptions = null, hideStatus = false, defaultOpen = true }) {
  const set = (field) => (event) => onChange({ ...value, [field]: event.target.value })
  const extraFields = hideStatus ? ['category', 'location', 'period'] : ['category', 'location', 'period', 'status']
  const extraCount = extraFields.filter((field) => value[field]).length
  const [open, setOpen] = useState(defaultOpen)
  const clearedValue = hideStatus ? { ...EMPTY_FILTERS, status: value.status } : EMPTY_FILTERS
  const anyFilter = Object.keys(value).some((key) => value[key] && !(hideStatus && key === 'status'))

  return (
    <div className="flex flex-col gap-3 rounded-md border border-line bg-card p-3" role="search">
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative min-w-56 flex-1">
          <Search
            size={18}
            className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2 text-ink-soft"
            aria-hidden="true"
          />
          <input
            type="search"
            aria-label="Search items"
            placeholder="Search by name or description"
            value={value.q}
            onChange={set('q')}
            className="w-full rounded-control border border-line bg-canvas py-2 pr-3 pl-10 text-base placeholder:text-ink-soft/70 hover:border-ink-soft focus:border-primary focus-visible:outline-offset-0"
          />
        </div>

        <SegmentedControl
          label="Type"
          options={TYPE_OPTIONS}
          value={value.type}
          onChange={(type) => onChange({ ...value, type })}
        />

        <Button variant="secondary" onClick={() => setOpen((current) => !current)} aria-expanded={open}>
          <SlidersHorizontal size={16} aria-hidden="true" /> Filters
          {extraCount > 0 && (
            <span className="inline-flex size-5 items-center justify-center rounded-full bg-primary text-xs text-on-primary">
              {extraCount}
            </span>
          )}
        </Button>
      </div>

      {open && (
        <div className={`grid gap-3 sm:grid-cols-2 ${hideStatus ? 'lg:grid-cols-3' : 'lg:grid-cols-4'}`}>
          <Select label="Category" value={value.category} onChange={set('category')}>
            <option value="">Any category</option>
            {CATEGORIES.map((name) => (
              <option key={name} value={name}>
                {name}
              </option>
            ))}
          </Select>
          <Input label="Location" placeholder="e.g. Library" value={value.location} onChange={set('location')} />
          <Select label="Reported" value={value.period} onChange={set('period')}>
            {PERIOD_OPTIONS.map(({ value: optionValue, label }) => (
              <option key={optionValue} value={optionValue}>
                {label}
              </option>
            ))}
          </Select>
          {!hideStatus && (
            <Select label="Status" value={value.status} onChange={set('status')}>
            <option value="">{allStatusesLabel}</option>
            {(statusOptions ?? STATUS_OPTIONS).map(({ value: optionValue, label }) => (
              <option key={optionValue} value={optionValue}>
                {label}
              </option>
            ))}
          </Select>
          )}
        </div>
      )}

      {anyFilter && (
        <div>
          <Button variant="ghost" size="sm" onClick={() => onChange(clearedValue)}>
            <X size={16} aria-hidden="true" /> Clear all filters
          </Button>
        </div>
      )}
    </div>
  )
}
