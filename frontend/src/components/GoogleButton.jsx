import { useEffect, useRef } from 'react'

import Button from './Button.jsx'

const CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID
const SCRIPT_URL = 'https://accounts.google.com/gsi/client'

// Load Google's script only once, however many times the button is shown.
let scriptPromise = null
function loadGoogleScript() {
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script')
      script.src = SCRIPT_URL
      script.async = true
      script.onload = resolve
      script.onerror = () => {
        scriptPromise = null
        reject(new Error('Could not load Google sign-in'))
      }
      document.head.appendChild(script)
    })
  }
  return scriptPromise
}

/**
 * "Continue with Google". Google draws the real button; when the user finishes, Google gives
 * us an ID token (a "credential") which we pass to onCredential. The backend then verifies it.
 * Without VITE_GOOGLE_CLIENT_ID in .env the button is shown disabled.
 */
export default function GoogleButton({ onCredential, onError, text = 'continue_with' }) {
  const containerRef = useRef(null)
  const callbackRef = useRef(onCredential)
  callbackRef.current = onCredential

  useEffect(() => {
    if (!CLIENT_ID) return
    let cancelled = false
    loadGoogleScript()
      .then(() => {
        if (cancelled || !containerRef.current) return
        window.google.accounts.id.initialize({
          client_id: CLIENT_ID,
          callback: (response) => callbackRef.current(response.credential),
        })
        window.google.accounts.id.renderButton(containerRef.current, {
          theme: 'outline',
          size: 'large',
          shape: 'rectangular',
          text,
          width: Math.min(400, containerRef.current.offsetWidth),
        })
      })
      .catch((error) => onError?.(error.message))
    return () => {
      cancelled = true
    }
  }, [text, onError])

  if (!CLIENT_ID) {
    return (
      <div className="flex flex-col gap-1">
        <Button variant="secondary" disabled className="w-full">
          Continue with Google
        </Button>
        <p className="text-center text-xs text-ink-soft">Google sign-in is not set up yet.</p>
      </div>
    )
  }

  return <div ref={containerRef} className="flex min-h-10 w-full justify-center" />
}
