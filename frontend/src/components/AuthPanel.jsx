// Centered card used by the login and register pages.
export default function AuthPanel({ title, subtitle, children, footer }) {
  return (
    <div className="mx-auto w-full max-w-md">
      <div className="mb-4 text-center">
        <h1>{title}</h1>
        {subtitle && <p className="text-sm text-ink-soft">{subtitle}</p>}
      </div>
      <div className="flex flex-col gap-4 rounded-card bg-card shadow-card p-5">{children}</div>
      {footer && <p className="mt-3 text-center text-sm text-ink-soft">{footer}</p>}
    </div>
  )
}
