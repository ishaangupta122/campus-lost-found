import { useCallback, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'

import AuthPanel from '../components/AuthPanel.jsx'
import Button from '../components/Button.jsx'
import GoogleButton from '../components/GoogleButton.jsx'
import Input from '../components/Input.jsx'
import { ErrorMessage } from '../components/States.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useNotification } from '../hooks/useNotification.js'
import { getApiError } from '../services/api.js'
import { usePageTitle } from '../hooks/usePageTitle.js'

export default function Register() {
  usePageTitle('Sign up')
  const { isAuthenticated, register, loginWithGoogle } = useAuth()
  const notify = useNotification()

  const [form, setForm] = useState({ name: '', email: '', password: '', confirm: '' })
  const [error, setError] = useState({ message: '', fieldErrors: {} })
  const [submitting, setSubmitting] = useState(false)

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  // Once the user is logged in this page redirects to the home page (see below).
  const finish = (newUser) => notify.success(`Welcome, ${newUser.name}`)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError({ message: '', fieldErrors: {} })

    // Checked in the browser only; the backend does not need the confirmation.
    if (form.password !== form.confirm) {
      setError({ message: '', fieldErrors: { confirm: 'Passwords do not match' } })
      return
    }

    setSubmitting(true)
    try {
      finish(await register(form.name.trim(), form.email.trim(), form.password))
    } catch (err) {
      setError(getApiError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleGoogle = async (idToken) => {
    setError({ message: '', fieldErrors: {} })
    try {
      finish(await loginWithGoogle(idToken))
    } catch (err) {
      setError(getApiError(err))
    }
  }

  const showGoogleLoadError = useCallback((message) => setError({ message, fieldErrors: {} }), [])

  if (isAuthenticated) return <Navigate to="/" replace />

  const hasFieldErrors = Object.keys(error.fieldErrors).length > 0

  return (
    <AuthPanel
      title="Create your account"
      subtitle="Join your campus community to report and claim items."
      footer={
        <>
          Already have an account? <Link to="/login">Log in</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        {error.message && !hasFieldErrors && <ErrorMessage message={error.message} />}
        <Input
          label="Full name"
          autoComplete="name"
          value={form.name}
          onChange={update('name')}
          error={error.fieldErrors.name}
          required
        />
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          value={form.email}
          onChange={update('email')}
          error={error.fieldErrors.email}
          required
        />
        <Input
          label="Password"
          type="password"
          autoComplete="new-password"
          hint="8 to 72 characters"
          value={form.password}
          onChange={update('password')}
          error={error.fieldErrors.password}
          required
        />
        <Input
          label="Confirm password"
          type="password"
          autoComplete="new-password"
          value={form.confirm}
          onChange={update('confirm')}
          error={error.fieldErrors.confirm}
          required
        />
        <Button type="submit" loading={submitting}>
          Create account
        </Button>
      </form>

      <div className="flex items-center gap-3 text-xs text-ink-soft uppercase">
        <span className="h-px flex-1 bg-line" /> or <span className="h-px flex-1 bg-line" />
      </div>
      <GoogleButton onCredential={handleGoogle} onError={showGoogleLoadError} text="signup_with" />
    </AuthPanel>
  )
}
