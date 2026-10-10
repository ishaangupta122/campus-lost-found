const API_BASE = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

// An item's imagePath is either a web link (older reports) or a path on our backend such as
// /api/uploads/files/abc.png. Turn it into something an <img src> can load.
export function imageUrl(imagePath) {
  if (!imagePath) return null
  if (/^https?:\/\//i.test(imagePath)) return imagePath
  return `${API_BASE}${imagePath}`
}
