import { useCallback, useState } from 'react'
import { Link, Navigate, useLocation } from 'react-router-dom'

import AuthPanel from '../components/AuthPanel.jsx'
import Button from '../components/Button.jsx'
import GoogleButton from '../components/GoogleButton.jsx'
import Input from '../components/Input.jsx'
import { ErrorMessage } from '../components/States.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useNotification } from '../hooks/useNotification.js'
import { getApiError } from '../services/api.js'
import { usePageTitle } from '../hooks/usePageTitle.js'

export default function Login() {
  usePageTitle('Log in')
  const { user, isAuthenticated, login, loginWithGoogle } = useAuth()
  const notify = useNotification()
  const location = useLocation()

  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState({ message: '', fieldErrors: {} })
  const [submitting, setSubmitting] = useState(false)

  const update = (field) => (event) => setForm({ ...form, [field]: event.target.value })

  // Once the user is logged in this page redirects (see below), so here we only say hello.
  const welcome = (loggedInUser) => notify.success(`Welcome, ${loggedInUser.name}`)

  const handleSubmit = async (event) => {
    event.preventDefault()
    setSubmitting(true)
    setError({ message: '', fieldErrors: {} })
    try {
      welcome(await login(form.email.trim(), form.password))
    } catch (err) {
      setError(getApiError(err))
    } finally {
      setSubmitting(false)
    }
  }

  const handleGoogle = async (idToken) => {
    setError({ message: '', fieldErrors: {} })
    try {
      welcome(await loginWithGoogle(idToken))
    } catch (err) {
      setError(getApiError(err))
    }
  }

  const showGoogleLoadError = useCallback((message) => setError({ message, fieldErrors: {} }), [])

  // Logged in: go back to the page the user wanted, or to their own home page.
  if (isAuthenticated) {
    return <Navigate to={location.state?.from ?? (user.role === 'ADMIN' ? '/admin' : '/')} replace />
  }

  const hasFieldErrors = Object.keys(error.fieldErrors).length > 0

  return (
    <AuthPanel
      title="Welcome back"
      subtitle="Log in to report items and track your claims."
      footer={
        <>
          New here? <Link to="/register">Create an account</Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-4" noValidate>
        {error.message && !hasFieldErrors && <ErrorMessage message={error.message} />}
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
          autoComplete="current-password"
          value={form.password}
          onChange={update('password')}
          error={error.fieldErrors.password}
          required
        />
        <Button type="submit" loading={submitting}>
          Log in
        </Button>
      </form>

      <div className="flex items-center gap-3 text-xs text-ink-soft uppercase">
        <span className="h-px flex-1 bg-line" /> or <span className="h-px flex-1 bg-line" />
      </div>
      <GoogleButton onCredential={handleGoogle} onError={showGoogleLoadError} text="signin_with" />
    </AuthPanel>
  )
}
