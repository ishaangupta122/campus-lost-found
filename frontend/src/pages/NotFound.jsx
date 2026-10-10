import { EmptyState } from '../components/States.jsx'

export default function NotFound() {
  return (
    <EmptyState
      title="Page not found"
      message="The page you are looking for does not exist."
      action={{ label: 'Back to home', to: '/' }}
    />
  )
}
