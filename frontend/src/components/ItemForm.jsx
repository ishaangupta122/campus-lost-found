import { useState } from 'react'
import { HandHeart, SearchX } from 'lucide-react'

import Button from './Button.jsx'
import ImageUpload from './ImageUpload.jsx'
import Input from './Input.jsx'
import Select from './Select.jsx'
import { ErrorMessage } from './States.jsx'
import { getApiError } from '../services/api.js'
import { CATEGORIES } from '../utils/constants.js'

// Today as YYYY-MM-DD in the user's own time zone (a report date cannot be in the future).
const today = () => new Date().toLocaleDateString('en-CA')

const TYPES = [
  { value: 'LOST', title: 'I lost something', hint: 'Help finders recognise it.', icon: SearchX },
  { value: 'FOUND', title: 'I found something', hint: 'Help the owner get it back.', icon: HandHeart },
]

/**
 * The form used to report a new item and to edit an existing one (pass `item` to edit).
 * onSubmit(data) must return a promise; errors from the backend are shown under the fields.
 */
export default function ItemForm({ item, submitLabel, onSubmit, onCancel }) {
  const editing = Boolean(item)
  const [form, setForm] = useState({
    itemName: item?.itemName ?? '',
    type: item?.type ?? 'LOST',
    category: item?.category ?? '',
    location: item?.location ?? '',
    description: item?.description ?? '',
    reportDate: item?.reportDate ?? today(),
    imagePath: item?.imagePath ?? null,
  })
  const [error, setError] = useState({ message: '', fieldErrors: {} })
  const [submitting, setSubmitting] = useState(false)

  const set = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  const handleSubmit = async (event) => {
    event.preventDefault()
    setSubmitting(true)
    setError({ message: '', fieldErrors: {} })

    const data = {
      itemName: form.itemName.trim(),
      category: form.category,
      location: form.location.trim(),
      description: form.description.trim(),
      reportDate: form.reportDate || null,
      imagePath: form.imagePath,
    }
    if (!editing) data.type = form.type // the type cannot be changed later

    try {
      await onSubmit(data)
    } catch (err) {
      setError(getApiError(err))
      setSubmitting(false)
    }
  }

  const hasFieldErrors = Object.keys(error.fieldErrors).length > 0
  // A category saved earlier that is not in today's list is still shown, so editing never loses it.
  const categories = form.category && !CATEGORIES.includes(form.category) ? [form.category, ...CATEGORIES] : CATEGORIES

  return (
    <form
      onSubmit={handleSubmit}
      className="flex flex-col gap-6 rounded-md border border-line bg-card p-5 sm:p-6"
      noValidate
    >
      {error.message && !hasFieldErrors && <ErrorMessage message={error.message} />}

      <div className="flex flex-col gap-2">
        <h2>{editing ? 'Report type' : 'What are you reporting?'}</h2>
        <div className="grid gap-3 sm:grid-cols-2" role="radiogroup" aria-label="Report type">
          {TYPES.map(({ value, title, hint, icon: Icon }) => {
            const selected = form.type === value
            return (
              <button
                key={value}
                type="button"
                role="radio"
                aria-checked={selected}
                disabled={editing && !selected}
                onClick={() => !editing && setForm({ ...form, type: value })}
                className={`flex items-start gap-3 rounded-md border-2 p-3 text-left transition-colors disabled:hidden ${
                  selected ? 'border-primary bg-primary-soft' : 'border-line bg-canvas hover:border-primary/50'
                } ${editing ? 'cursor-default' : 'cursor-pointer'}`}
              >
                <span
                  className={`inline-flex size-9 shrink-0 items-center justify-center rounded-full ${
                    selected ? 'bg-primary text-on-primary' : 'bg-primary-soft text-primary'
                  }`}
                >
                  <Icon size={18} aria-hidden="true" />
                </span>
                <span>
                  <span className="block text-sm font-semibold text-ink">{title}</span>
                  <span className="block text-sm text-ink-soft">{hint}</span>
                </span>
              </button>
            )
          })}
        </div>
        {editing && <p className="text-sm text-ink-soft">The type cannot be changed after the report is created.</p>}
      </div>

      <div className="grid gap-6 md:grid-cols-[1fr_320px]">
        <div className="flex flex-col gap-6">
          <section className="flex flex-col gap-3" aria-labelledby="details-heading">
            <h2 id="details-heading">Item details</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <Input
                label="Item name"
                placeholder="e.g. Black backpack"
                value={form.itemName}
                onChange={set('itemName')}
                error={error.fieldErrors.itemName}
                required
              />
              <Select label="Category" value={form.category} onChange={set('category')} error={error.fieldErrors.category} required>
                <option value="">Choose a category</option>
                {categories.map((name) => (
                  <option key={name} value={name}>
                    {name}
                  </option>
                ))}
              </Select>
            </div>
            <Input
              label="Description"
              multiline
              rows={3}
              hint="Colour, brand, marks. Keep back one detail only the owner would know."
              value={form.description}
              onChange={set('description')}
              error={error.fieldErrors.description}
              required
            />
          </section>

          <section className="flex flex-col gap-3" aria-labelledby="where-heading">
            <h2 id="where-heading">Where and when</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <Input
                label={form.type === 'LOST' ? 'Where did you lose it?' : 'Where did you find it?'}
                placeholder="e.g. Central Library"
                value={form.location}
                onChange={set('location')}
                error={error.fieldErrors.location}
                required
              />
              <Input
                label={form.type === 'LOST' ? 'Date lost' : 'Date found'}
                type="date"
                max={today()}
                value={form.reportDate}
                onChange={set('reportDate')}
                error={error.fieldErrors.reportDate}
              />
            </div>
          </section>
        </div>

        <ImageUpload
          value={form.imagePath}
          onChange={(imagePath) => setForm({ ...form, imagePath })}
          error={error.fieldErrors.imagePath}
        />
      </div>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-line pt-4">
        <p className="text-sm text-ink-soft">
          <span className="text-danger" aria-hidden="true">
            *
          </span>{' '}
          Required
        </p>
        <div className="flex gap-2">
          {onCancel && (
            <Button variant="secondary" onClick={onCancel} disabled={submitting}>
              Cancel
            </Button>
          )}
          <Button type="submit" loading={submitting}>
            {submitLabel}
          </Button>
        </div>
      </div>
    </form>
  )
}
