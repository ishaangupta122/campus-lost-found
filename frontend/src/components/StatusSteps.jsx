import { Check } from 'lucide-react'

const STEPS = ['Reported', 'Claim pending', 'Returned']
// Which step is the current one for each status
const CURRENT = { OPEN: 0, CLAIM_PENDING: 1, RETURNED: 2 }

/** A small progress line showing where an item is in its life: Reported -> Claim pending -> Returned. */
export default function StatusSteps({ status }) {
  if (status === 'CLOSED') {
    return <p className="rounded-control bg-stone-bg px-3 py-2 text-sm text-stone-fg">This report was closed.</p>
  }
  const current = CURRENT[status] ?? 0

  return (
    <ol className="flex items-center" aria-label="Progress">
      {STEPS.map((label, index) => {
        const done = index < current || status === 'RETURNED'
        const active = index === current
        return (
          <li key={label} className="flex flex-1 items-center last:flex-none">
            <span className="flex items-center gap-2">
              <span
                className={`inline-flex size-6 items-center justify-center rounded-full text-xs font-semibold ${
                  done
                    ? 'bg-primary text-on-primary'
                    : active
                      ? 'border-2 border-primary text-primary'
                      : 'border border-line text-ink-soft'
                }`}
                aria-current={active ? 'step' : undefined}
              >
                {done ? <Check size={14} aria-hidden="true" /> : index + 1}
              </span>
              <span className={`text-sm ${active || done ? 'font-medium text-ink' : 'text-ink-soft'}`}>{label}</span>
            </span>
            {index < STEPS.length - 1 && <span className={`mx-3 h-px flex-1 ${done ? 'bg-primary' : 'bg-line'}`} />}
          </li>
        )
      })}
    </ol>
  )
}
