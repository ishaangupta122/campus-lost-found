import { useEffect, useState } from 'react'

import { getApiError } from '../services/api.js'

/**
 * Loads data when a page opens (and again when `deps` change).
 *   const { data, loading, error, reload } = useFetch(() => getMyItems(), [])
 * `fetcher` must return a promise (one of the service functions).
 */
export function useFetch(fetcher, deps = []) {
  const [state, setState] = useState({ data: null, loading: true, error: '' })
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false // ignore the answer if the page changed meanwhile
    setState((current) => ({ ...current, loading: true, error: '' }))
    fetcher()
      .then((data) => !cancelled && setState({ data, loading: false, error: '' }))
      .catch((error) => !cancelled && setState({ data: null, loading: false, error: getApiError(error).message }))
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, reloadKey])

  return { ...state, reload: () => setReloadKey((key) => key + 1) }
}
