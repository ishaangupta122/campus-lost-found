// Card centred on the page (horizontally and vertically), used by the login and register pages.
export default function AuthPanel({ title, subtitle, children, footer }) {
  return (
    <div className="flex min-h-[calc(100svh-9rem)] items-center justify-center">
      <div className="w-full max-w-md">
        <div className="flex flex-col gap-5 rounded-md border border-line bg-card p-6">
          <div className="text-center">
            <h1>{title}</h1>
            {subtitle && <p className="mt-1 text-sm text-ink-soft">{subtitle}</p>}
          </div>
          {children}
        </div>
        {footer && <p className="mt-4 text-center text-sm text-ink-soft">{footer}</p>}
      </div>
    </div>
  )
}
