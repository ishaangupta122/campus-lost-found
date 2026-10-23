import { useEffect, useRef, useState } from 'react'

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
 * While Google's script loads (or without VITE_GOOGLE_CLIENT_ID in .env) a disabled placeholder
 * button holds the space, so the page does not jump when the real button appears.
 */
export default function GoogleButton({ onCredential, onError, text = 'continue_with' }) {
  const wrapperRef = useRef(null)
  const containerRef = useRef(null)
  const callbackRef = useRef(onCredential)
  callbackRef.current = onCredential
  const [status, setStatus] = useState(CLIENT_ID ? 'loading' : 'missing')

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
          width: Math.min(400, wrapperRef.current.offsetWidth),
        })
        setStatus('ready')
      })
      .catch((error) => {
        if (cancelled) return
        setStatus('failed')
        onError?.(error.message)
      })
    return () => {
      cancelled = true
    }
  }, [text, onError])

  return (
    <div ref={wrapperRef} className="flex w-full flex-col gap-1">
      {status !== 'ready' && (
        <>
          <Button variant="secondary" disabled className={`w-full ${status === 'loading' ? 'animate-pulse' : ''}`}>
            Continue with Google
          </Button>
          {status === 'missing' && <p className="text-center text-xs text-ink-soft">Google sign-in is not set up yet.</p>}
          {status === 'failed' && <p className="text-center text-xs text-ink-soft">Google sign-in is unavailable right now.</p>}
        </>
      )}
      <div ref={containerRef} className={status === 'ready' ? 'flex min-h-10 w-full justify-center' : 'hidden'} />
    </div>
  )
}
