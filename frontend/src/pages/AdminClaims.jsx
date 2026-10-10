import { useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { Check, X } from 'lucide-react'

import Button from '../components/Button.jsx'
import ClaimCard from '../components/ClaimCard.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import PageHeader from '../components/PageHeader.jsx'
import { EmptyState, ErrorMessage, LoadingState } from '../components/States.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { useNotification } from '../hooks/useNotification.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { decideClaim, getPendingClaims } from '../services/adminService.js'
import { getApiError } from '../services/api.js'

export default function AdminClaims() {
  usePageTitle('Pending claims')
  const { reloadStats } = useOutletContext()
  const notify = useNotification()
  const { data: claims, loading, error, reload } = useFetch(() => getPendingClaims(), [])
  const [pending, setPending] = useState(null) // { claim, decision }
  const [working, setWorking] = useState(false)

  const handleConfirm = async () => {
    setWorking(true)
    try {
      await decideClaim(pending.claim.id, pending.decision)
      notify.success(pending.decision === 'APPROVED' ? 'Claim approved' : 'Claim rejected')
      reload()
      reloadStats()
    } catch (err) {
      notify.error(getApiError(err).message)
      reload() // the claim may already have been decided by someone else
      reloadStats()
    } finally {
      setWorking(false)
      setPending(null)
    }
  }

  const approving = pending?.decision === 'APPROVED'

  return (
    <>
      <PageHeader title="Pending claims" subtitle="Oldest first. Check the message before you decide." />

      {loading && !claims && <LoadingState message="Loading claims..." />}
      {error && <ErrorMessage message={error} onRetry={reload} />}
      {claims && claims.length === 0 && (
        <EmptyState title="All caught up" message="There are no claims waiting for a decision." />
      )}
      {claims && claims.length > 0 && (
        <div className="flex flex-col gap-4">
          {claims.map((claim) => (
            <ClaimCard key={claim.id} claim={claim} showClaimant>
              <Button size="sm" onClick={() => setPending({ claim, decision: 'APPROVED' })}>
                <Check size={16} aria-hidden="true" /> Approve
              </Button>
              <Button size="sm" variant="secondary" onClick={() => setPending({ claim, decision: 'REJECTED' })}>
                <X size={16} aria-hidden="true" /> Reject
              </Button>
            </ClaimCard>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={pending !== null}
        title={approving ? 'Approve this claim?' : 'Reject this claim?'}
        message={
          approving
            ? `${pending?.claim.claimantName} will be able to collect "${pending?.claim.itemName}". Other waiting claims for it will be rejected.`
            : `${pending?.claim.claimantName}'s claim for "${pending?.claim.itemName}" will be rejected.`
        }
        confirmLabel={approving ? 'Approve' : 'Reject'}
        danger={!approving}
        loading={working}
        onConfirm={handleConfirm}
        onCancel={() => !working && setPending(null)}
      />
    </>
  )
}
