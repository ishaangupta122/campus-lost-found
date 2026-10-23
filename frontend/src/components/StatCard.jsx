import { Link } from 'react-router-dom'

/** One number with a label. Pass `to` to make the whole card a link (e.g. to a filtered list). */
export default function StatCard({ label, value, icon: Icon, to }) {
  const content = (
    <>
      <p className="flex items-center gap-2 text-sm text-ink-soft">
        {Icon && <Icon size={16} className="text-primary" aria-hidden="true" />}
        {label}
      </p>
      <p className="text-2xl font-semibold text-ink">{value}</p>
    </>
  )
  const classes = 'flex flex-col gap-1 rounded-md border border-line bg-card px-4 py-3'

  if (!to) return <div className={classes}>{content}</div>
  return (
    <Link to={to} className={`${classes} transition-colors hover:border-primary hover:no-underline`}>
      {content}
    </Link>
  )
}
