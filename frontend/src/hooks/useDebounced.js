import { useEffect, useState } from 'react'

// Returns `value` only after it has stopped changing for `delay` ms (used so search runs
// after the user stops typing, not on every key press).
export function useDebounced(value, delay = 350) {
  const [debounced, setDebounced] = useState(value)
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay)
    return () => clearTimeout(timer)
  }, [value, delay])
  return debounced
}
