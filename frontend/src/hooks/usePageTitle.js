import { useEffect } from 'react'

// Sets the browser tab title, e.g. usePageTitle('My claims') -> "My claims | Campus Lost & Found"
export function usePageTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} | Campus Lost & Found` : 'Campus Lost & Found'
  }, [title])
}
