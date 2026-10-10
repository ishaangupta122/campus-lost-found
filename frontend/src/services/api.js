import axios from 'axios'

// The ONE Axios client of the app. Every service module uses it, so the base URL,
// token handling and error format are defined in exactly one place.
export const TOKEN_KEY = 'lf_token'
export const AUTH_EXPIRED_EVENT = 'auth:expired'

// No default Content-Type on purpose: Axios sends JSON by itself for normal objects, and for a
// FormData (image upload) it lets the browser set "multipart/form-data" with its boundary.
// A fixed "application/json" here would turn the uploaded file into JSON and the upload would fail.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
})

// Before every request: attach the JWT if we have one.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// After every response: turn any failure into one simple shape, error.apiError =
// { status, message, fieldErrors }. The backend always sends { message, fieldErrors? }.
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const data = error.response?.data
    error.apiError = {
      status: error.response?.status ?? 0,
      message: data?.message ?? 'Cannot reach the server. Please check your connection and try again.',
      fieldErrors: data?.fieldErrors ?? {},
    }

    // A 401 on a protected call means the token is missing, expired or invalid.
    // (Login/register also return 401 for wrong details, so skip the /api/auth/ calls
    // except /me.) AuthContext listens for this event and logs the user out.
    const url = error.config?.url ?? ''
    const isLoginAttempt = url.startsWith('/api/auth/') && !url.endsWith('/me')
    if (error.apiError.status === 401 && !isLoginAttempt) {
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
    }
    return Promise.reject(error)
  },
)

// Components use this to read the friendly error that the interceptor attached.
export function getApiError(error) {
  return error.apiError ?? { status: 0, message: 'Something went wrong.', fieldErrors: {} }
}

// Removes empty filter values so we never send "?q=&type=" to the backend.
export function cleanParams(params = {}) {
  return Object.fromEntries(Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined))
}

export default api
