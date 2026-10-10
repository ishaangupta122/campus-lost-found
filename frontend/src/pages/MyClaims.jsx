import ClaimCard from '../components/ClaimCard.jsx'
import PageHeader from '../components/PageHeader.jsx'
import { EmptyState, ErrorMessage, LoadingState } from '../components/States.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { getMyClaims } from '../services/claimService.js'
import { usePageTitle } from '../hooks/usePageTitle.js'

export default function MyClaims() {
  usePageTitle('My claims')
  const { data: claims, loading, error, reload } = useFetch(() => getMyClaims(), [])

  return (
    <>
      <PageHeader title="My claims" subtitle="Items you have asked to claim, and what happened to them." />

      {loading && !claims && <LoadingState message="Loading your claims..." />}
      {error && <ErrorMessage message={error} onRetry={reload} />}
      {claims && claims.length === 0 && (
        <EmptyState
          title="No claims yet"
          message="Found your lost item in the list? Open it and submit a claim."
          action={{ label: 'Browse items', to: '/' }}
        />
      )}
      {claims && claims.length > 0 && (
        <div className="flex flex-col gap-3">
          {claims.map((claim) => (
            <ClaimCard key={claim.id} claim={claim} />
          ))}
        </div>
      )}
    </>
  )
}
