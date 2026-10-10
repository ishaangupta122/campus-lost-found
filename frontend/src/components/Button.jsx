import { Link } from 'react-router-dom'
import { LoaderCircle } from 'lucide-react'

const BASE =
  'inline-flex items-center justify-center gap-2 rounded-control border font-medium leading-tight whitespace-nowrap cursor-pointer transition-colors no-underline hover:no-underline disabled:cursor-not-allowed disabled:opacity-55'

const VARIANTS = {
  primary: 'border-transparent bg-primary text-on-primary not-disabled:hover:bg-primary-hover',
  secondary: 'border-line bg-card text-ink not-disabled:hover:bg-sand',
  danger: 'border-transparent bg-danger text-on-primary not-disabled:hover:bg-danger-hover',
  ghost: 'border-transparent bg-transparent text-ink-soft not-disabled:hover:bg-sand not-disabled:hover:text-ink',
}

const SIZES = {
  md: 'px-4 py-2 text-sm',
  sm: 'px-3 py-1 text-sm',
}

/**
 * variant: primary | secondary | danger | ghost     size: md | sm
 * loading: shows a spinner and blocks clicks.   to: renders a router Link instead of a <button>.
 */
export default function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled = false,
  to,
  type = 'button',
  className = '',
  children,
  ...rest
}) {
  const classes = `${BASE} ${VARIANTS[variant]} ${SIZES[size]} ${className}`.trim()

  if (to) {
    return (
      <Link to={to} className={classes} {...rest}>
        {children}
      </Link>
    )
  }

  return (
    <button type={type} className={classes} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      {loading && <LoaderCircle className="animate-spin" size={16} aria-hidden="true" />}
      {children}
    </button>
  )
}
