import { Link } from 'react-router-dom'

/** One number with a label. Pass `to` to make the whole card a link (e.g. to a filtered list). */
export default function StatCard({ label, value, icon: Icon, to }) {
  const content = (
    <>
      {Icon && <Icon size={18} className="text-primary" aria-hidden="true" />}
      <p className="text-2xl font-semibold text-ink">{value}</p>
      <p className="text-sm text-ink-soft">{label}</p>
    </>
  )
  const classes = 'flex flex-col gap-0.5 rounded-card bg-card shadow-card p-3'

  if (!to) return <div className={classes}>{content}</div>
  return (
    <Link to={to} className={`${classes} transition-colors hover:border-primary hover:no-underline`}>
      {content}
    </Link>
  )
}
