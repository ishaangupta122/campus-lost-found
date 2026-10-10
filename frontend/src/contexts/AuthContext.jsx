import { createContext, useCallback, useEffect, useMemo, useState } from 'react'

import * as authService from '../services/authService.js'
import { AUTH_EXPIRED_EVENT, TOKEN_KEY } from '../services/api.js'

export const AuthContext = createContext(null)

/**
 * Holds the logged-in user for the whole app. The JWT lives in localStorage so a page
 * refresh keeps you logged in; api.js reads it from there and attaches it to requests.
 */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  // true until we have checked whether the stored token (if any) is still valid
  const [initializing, setInitializing] = useState(() => Boolean(localStorage.getItem(TOKEN_KEY)))

  const clearSession = useCallback(() => {
    localStorage.removeItem(TOKEN_KEY)
    setUser(null)
  }, [])

  // On first load: if a token exists, ask the backend who we are.
  useEffect(() => {
    if (!localStorage.getItem(TOKEN_KEY)) return
    authService
      .getMe()
      .then(setUser)
      .catch(clearSession)
      .finally(() => setInitializing(false))
  }, [clearSession])

  // api.js fires this event when the backend answers 401 to a protected request.
  useEffect(() => {
    window.addEventListener(AUTH_EXPIRED_EVENT, clearSession)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, clearSession)
  }, [clearSession])

  // Saves the token and user from a login response: { token, user }
  const startSession = useCallback((authResponse) => {
    localStorage.setItem(TOKEN_KEY, authResponse.token)
    setUser(authResponse.user)
    return authResponse.user
  }, [])

  const login = useCallback((email, password) => authService.login(email, password).then(startSession), [startSession])

  // Registering does not log you in by itself, so we log in right after.
  const register = useCallback(
    async (name, email, password) => {
      await authService.register(name, email, password)
      return login(email, password)
    },
    [login],
  )

  const loginWithGoogle = useCallback((idToken) => authService.loginWithGoogle(idToken).then(startSession), [startSession])

  const value = useMemo(
    () => ({
      user,
      initializing,
      isAuthenticated: user !== null,
      isAdmin: user?.role === 'ADMIN',
      isStudent: user?.role === 'STUDENT',
      login,
      register,
      loginWithGoogle,
      logout: clearSession,
    }),
    [user, initializing, login, register, loginWithGoogle, clearSession],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
