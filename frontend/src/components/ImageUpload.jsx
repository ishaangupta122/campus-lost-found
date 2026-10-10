import { useRef, useState } from 'react'
import { ImagePlus, LoaderCircle, Trash2 } from 'lucide-react'

import Button from './Button.jsx'
import { uploadImage } from '../services/uploadService.js'
import { getApiError } from '../services/api.js'
import { IMAGE_TYPES, MAX_IMAGE_MB } from '../utils/constants.js'
import { imageUrl } from '../utils/image.js'

/**
 * Choose a picture (click or drag and drop). It is uploaded right away and the backend returns
 * its path; the parent keeps that path in the form (value / onChange).
 */
export default function ImageUpload({ value, onChange, error }) {
  const inputRef = useRef(null)
  const [uploading, setUploading] = useState(false)
  const [localError, setLocalError] = useState('')
  const [dragging, setDragging] = useState(false)

  const handleFile = async (file) => {
    if (!file) return
    setLocalError('')
    // Quick checks in the browser; the backend checks again (it is the real gatekeeper).
    if (!IMAGE_TYPES.includes(file.type)) {
      setLocalError('Please choose a JPG, PNG, GIF or WebP image.')
      return
    }
    if (file.size > MAX_IMAGE_MB * 1024 * 1024) {
      setLocalError(`The image is too large. The limit is ${MAX_IMAGE_MB} MB.`)
      return
    }
    setUploading(true)
    try {
      onChange(await uploadImage(file))
    } catch (err) {
      setLocalError(getApiError(err).message)
    } finally {
      setUploading(false)
      if (inputRef.current) inputRef.current.value = '' // allows choosing the same file again
    }
  }

  const message = localError || error

  return (
    <div className="flex flex-col gap-1">
      <span className="text-sm font-medium">Photo (optional)</span>

      {value ? (
        <div className="flex flex-1 flex-col gap-2">
          <img src={imageUrl(value)} alt="Selected item" className="h-36 w-full rounded-control object-cover" />
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" size="sm" onClick={() => inputRef.current?.click()} loading={uploading}>
              Replace
            </Button>
            <Button variant="ghost" size="sm" onClick={() => onChange(null)} disabled={uploading}>
              <Trash2 size={16} aria-hidden="true" /> Remove
            </Button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          onDragOver={(event) => {
            event.preventDefault()
            setDragging(true)
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(event) => {
            event.preventDefault()
            setDragging(false)
            handleFile(event.dataTransfer.files[0])
          }}
          disabled={uploading}
          className={`flex min-h-36 flex-1 cursor-pointer flex-col items-center justify-center gap-1 rounded-control border-2 border-dashed px-4 py-3 text-center transition-colors ${
            dragging ? 'border-primary bg-primary-soft' : 'border-line bg-sand/50 hover:border-ink-soft'
          }`}
        >
          {uploading ? (
            <LoaderCircle className="animate-spin text-ink-soft" size={24} aria-hidden="true" />
          ) : (
            <ImagePlus className="text-ink-soft" size={24} aria-hidden="true" />
          )}
          <span className="text-sm font-medium">{uploading ? 'Uploading...' : 'Add a photo'}</span>
          <span className="text-xs text-ink-soft">Click or drop here. Up to {MAX_IMAGE_MB} MB</span>
        </button>
      )}

      <input
        ref={inputRef}
        type="file"
        accept={IMAGE_TYPES.join(',')}
        className="hidden"
        onChange={(event) => handleFile(event.target.files[0])}
        aria-label="Choose a photo"
      />
      {message && <p className="text-xs text-error-fg">{message}</p>}
    </div>
  )
}
