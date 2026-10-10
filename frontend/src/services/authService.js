import api from './api.js'

// Each function returns the response body (res.data), not the whole Axios response.
export const register = (name, email, password) =>
  api.post('/api/auth/register', { name, email, password }).then((res) => res.data)

// Returns { token, tokenType, expiresInSeconds, user }
export const login = (email, password) => api.post('/api/auth/login', { email, password }).then((res) => res.data)

export const loginWithGoogle = (idToken) => api.post('/api/auth/google', { idToken }).then((res) => res.data)

export const getMe = () => api.get('/api/auth/me').then((res) => res.data)
