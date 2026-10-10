// Grey placeholder cards shown while a list loads (calmer than a spinner).
export function ItemGridSkeleton({ count = 6 }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3" aria-busy="true" aria-label="Loading items">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className="animate-pulse overflow-hidden rounded-card bg-card shadow-card">
          <div className="aspect-video bg-sand" />
          <div className="flex flex-col gap-3 p-4">
            <div className="h-4 w-1/3 rounded bg-sand" />
            <div className="h-5 w-3/4 rounded bg-sand" />
            <div className="h-4 w-full rounded bg-sand" />
          </div>
        </div>
      ))}
    </div>
  )
}
