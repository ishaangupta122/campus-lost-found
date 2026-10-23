import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'

/** A bordered box with a tinted header strip (title and optional action on the right). */
export default function Panel({ title, action, children, className = '' }) {
  return (
    <section className={`overflow-hidden rounded-md border border-line bg-card ${className}`.trim()}>
      <header className="flex items-center justify-between gap-3 border-b border-line bg-sand px-4 py-2.5">
        <h2 className="text-base">{title}</h2>
        {action}
      </header>
      {children}
    </section>
  )
}

/** "See all" style link for a panel header. */
export function PanelLink({ to, children }) {
  return (
    <Link to={to} className="inline-flex items-center gap-1 text-sm">
      {children} <ArrowRight size={14} aria-hidden="true" />
    </Link>
  )
}
