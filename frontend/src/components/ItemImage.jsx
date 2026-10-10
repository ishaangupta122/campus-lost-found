import { useState } from 'react'

import { imageUrl } from '../utils/image.js'

// Shown when a report has no photo, or when its photo cannot be loaded.
export const FALLBACK_IMAGE = '/placeholder-item.svg'

/** An item's photo, or the fallback picture. className sizes it (e.g. "size-full object-cover"). */
export default function ItemImage({ imagePath, alt = '', className = '' }) {
  const [failedPath, setFailedPath] = useState(null)
  const src = imagePath && failedPath !== imagePath ? imageUrl(imagePath) : FALLBACK_IMAGE

  return <img src={src} alt={alt} loading="lazy" className={className} onError={() => setFailedPath(imagePath)} />
}
