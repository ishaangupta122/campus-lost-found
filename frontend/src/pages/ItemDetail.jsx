import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { Calendar, ChevronRight, MapPin, Tag, User } from 'lucide-react'

import Button from '../components/Button.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import ItemImage from '../components/ItemImage.jsx'
import Input from '../components/Input.jsx'
import StatusBadge from '../components/StatusBadge.jsx'
import StatusSteps from '../components/StatusSteps.jsx'
import { ErrorMessage, LoadingState } from '../components/States.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useFetch } from '../hooks/useFetch.js'
import { useNotification } from '../hooks/useNotification.js'
import { usePageTitle } from '../hooks/usePageTitle.js'
import { closeItemAsAdmin } from '../services/adminService.js'
import { getApiError } from '../services/api.js'
import { getMyClaims, submitClaim } from '../services/claimService.js'
import { closeItem, deleteItem, getItem, markItemReturned } from '../services/itemService.js'
import { formatDate } from '../utils/format.js'

// What each confirmation popup says and does.
const CONFIRMATIONS = {
  close: {
    title: 'Close this report?',
    message: 'Waiting claims will be rejected and the report will no longer be listed.',
    confirmLabel: 'Close report',
    danger: true,
  },
  delete: {
    title: 'Delete this report?',
    message: 'This cannot be undone. A report that has claims cannot be deleted; close it instead.',
    confirmLabel: 'Delete',
    danger: true,
  },
  returned: {
    title: 'Mark as returned?',
    message: 'Confirm that the item has been handed over to its owner.',
    confirmLabel: 'Mark returned',
    danger: false,
  },
}

const isActive = (item) => item.status === 'OPEN' || item.status === 'CLAIM_PENDING'

export default function ItemDetail() {
  const { id } = useParams()
  const { user, isAuthenticated, isAdmin, isStudent } = useAuth()
  const notify = useNotification()
  const navigate = useNavigate()
  const location = useLocation()

  const { data: item, loading, error, reload } = useFetch(() => getItem(id), [id])
  usePageTitle(item?.itemName)
  // Students: my claims, to know whether I already claimed this item.
  const { data: myClaims, reload: reloadClaims } = useFetch(() => (isStudent ? getMyClaims() : Promise.resolve([])), [isStudent])

  const [message, setMessage] = useState('')
  const [claimError, setClaimError] = useState({ message: '', fieldErrors: {} })
  const [claiming, setClaiming] = useState(false)
  const [confirming, setConfirming] = useState(null) // 'close' | 'delete' | 'returned' | null
  const [working, setWorking] = useState(false)

  // Cards link here with #claim: scroll to the claim box once the item has loaded.
  const loaded = Boolean(item)
  useEffect(() => {
    if (loaded && location.hash === '#claim') document.getElementById('claim')?.scrollIntoView({ block: 'center' })
  }, [loaded, location.hash])

  if (loading && !item) return <LoadingState message="Loading item..." />
  if (error) return <ErrorMessage message={error} onRetry={reload} />
  if (!item) return null

  const isOwner = isAuthenticated && user.id === item.reporterId
  const active = isActive(item)
  const claimsOnThisItem = (myClaims ?? []).filter((claim) => claim.itemId === item.id)
  const latestClaim = claimsOnThisItem[0] // the backend lists newest first
  const hasLiveClaim = claimsOnThisItem.some((claim) => claim.status === 'PENDING' || claim.status === 'APPROVED')
  const canClaim = isStudent && !isOwner && item.type === 'FOUND' && active && !hasLiveClaim
  const canMarkReturned = (isOwner || isAdmin) && item.status === 'CLAIM_PENDING'

  const handleClaim = async (event) => {
    event.preventDefault()
    setClaiming(true)
    setClaimError({ message: '', fieldErrors: {} })
    try {
      await submitClaim(item.id, message.trim())
      notify.success('Claim submitted. An admin will review it.')
      setMessage('')
      reload()
      reloadClaims()
    } catch (err) {
      setClaimError(getApiError(err))
    } finally {
      setClaiming(false)
    }
  }

  const handleConfirm = async () => {
    setWorking(true)
    try {
      if (confirming === 'close') {
        await (isAdmin ? closeItemAsAdmin(item.id) : closeItem(item.id))
        notify.success('Report closed')
      } else if (confirming === 'returned') {
        await markItemReturned(item.id)
        notify.success('Item marked as returned')
      } else if (confirming === 'delete') {
        await deleteItem(item.id)
        notify.success('Report deleted')
        navigate('/my-items', { replace: true })
        return
      }
      setConfirming(null)
      reload()
    } catch (err) {
      notify.error(getApiError(err).message)
      setConfirming(null)
    } finally {
      setWorking(false)
    }
  }

  const canManage = isOwner || isAdmin

  return (
    <div className="flex flex-col gap-3">
      <nav aria-label="Breadcrumb" className="flex items-center gap-1 text-sm text-ink-soft">
        <Link to="/" className="text-ink-soft hover:text-ink">
          Browse
        </Link>
        <ChevronRight size={14} aria-hidden="true" />
        <span className="truncate text-ink">{item.itemName}</span>
      </nav>

      <div className="grid items-start gap-4 lg:grid-cols-[1fr_340px]">
        <article className="flex flex-col overflow-hidden rounded-card bg-card shadow-card">
          <ItemImage imagePath={item.imagePath} alt={item.itemName} className="h-48 w-full bg-sand object-cover sm:h-64" />

          <div className="flex flex-col gap-3 p-5">
            <div className="flex gap-2">
              <StatusBadge value={item.type} />
              <StatusBadge value={item.status} />
            </div>
            <h1>{item.itemName}</h1>

            {item.type === 'FOUND' && <StatusSteps status={item.status} />}

            <p className="whitespace-pre-line">{item.description}</p>

            <ul className="grid gap-1.5 pt-1 text-sm text-ink-soft sm:grid-cols-2">
              <li className="inline-flex items-center gap-2">
                <MapPin size={16} aria-hidden="true" /> {item.location}
              </li>
              <li className="inline-flex items-center gap-2">
                <Tag size={16} aria-hidden="true" /> {item.category}
              </li>
              <li className="inline-flex items-center gap-2">
                <Calendar size={16} aria-hidden="true" /> {formatDate(item.reportDate)}
              </li>
              <li className="inline-flex items-center gap-2">
                <User size={16} aria-hidden="true" /> Reported by {item.reporterName}
              </li>
            </ul>
          </div>
        </article>

        <aside className="flex flex-col gap-3">
          {/* Claim area */}
          {item.type === 'FOUND' && (
            <section id="claim" className="flex flex-col gap-3 rounded-card bg-card shadow-card p-4">
              <h2 className="text-lg">Is this yours?</h2>
              <p className="text-sm text-ink-soft">
                Send a claim describing something only the owner would know. An admin reviews it, then you collect the item.
              </p>

              {!isAuthenticated && (
                <>
                  <p className="text-sm text-ink-soft">Log in to claim this item.</p>
                  <Button to="/login" state={{ from: `/items/${item.id}` }}>
                    Log in to claim
                  </Button>
                </>
              )}

              {isAdmin && (
                <p className="text-sm text-ink-soft">Claims are submitted by students. Admins decide them in the admin area.</p>
              )}

              {isStudent && isOwner && <p className="text-sm text-ink-soft">You reported this item, so you cannot claim it.</p>}

              {isStudent && !isOwner && latestClaim && hasLiveClaim && (
                <div className="flex flex-col gap-2 text-sm">
                  <p className="inline-flex items-center gap-2">
                    Your claim: <StatusBadge value={latestClaim.status} />
                  </p>
                  <p className="text-ink-soft">
                    {latestClaim.status === 'APPROVED'
                      ? 'Your claim was approved. Contact the person who found it to collect your item.'
                      : 'An admin will review your claim soon.'}
                  </p>
                </div>
              )}

              {isStudent && !isOwner && !active && !hasLiveClaim && (
                <p className="text-sm text-ink-soft">
                  This item is {item.status.replace('_', ' ').toLowerCase()} and can no longer be claimed.
                </p>
              )}

              {canClaim && (
                <form onSubmit={handleClaim} className="flex flex-col gap-3" noValidate>
                  {latestClaim?.status === 'REJECTED' && (
                    <p className="text-sm text-ink-soft">Your earlier claim was rejected. You can submit a new one.</p>
                  )}
                  {claimError.message && !claimError.fieldErrors.message && <ErrorMessage message={claimError.message} />}
                  <Input
                    label="Why is it yours?"
                    multiline
                    hint="Describe something only the owner would know."
                    value={message}
                    onChange={(event) => setMessage(event.target.value)}
                    error={claimError.fieldErrors.message}
                    required
                  />
                  <Button type="submit" loading={claiming}>
                    Submit claim
                  </Button>
                </form>
              )}
            </section>
          )}

          {item.type === 'LOST' && (
            <section className="flex flex-col gap-2 rounded-card bg-card shadow-card p-4">
              <h2 className="text-lg">Seen this item?</h2>
              <p className="text-sm text-ink-soft">
                Lost reports cannot be claimed. If you found it, report it as a found item and say where you left it.
                The owner can then claim it.
              </p>
              {!isAuthenticated && (
                <Button to="/login" state={{ from: `/items/${item.id}` }} variant="secondary">
                  Log in to help
                </Button>
              )}
              {isStudent && !isOwner && (
                <Button to="/report" variant="secondary">
                  Report a found item
                </Button>
              )}
            </section>
          )}

          {/* Manage area: owner and admin */}
          {canManage && (
            <section className="flex flex-col gap-3 rounded-card bg-card shadow-card p-4">
              <h2 className="text-lg">Manage report</h2>
              {item.status === 'CLAIM_PENDING' && (
                <p className="text-sm text-ink-soft">
                  After an admin approves a claim and the item is handed over, mark it as returned.
                </p>
              )}
              <div className="flex flex-wrap gap-2">
                {isOwner && active && (
                  <Button to={`/items/${item.id}/edit`} variant="secondary">
                    Edit
                  </Button>
                )}
                {canMarkReturned && <Button onClick={() => setConfirming('returned')}>Mark returned</Button>}
                {active && (
                  <Button variant="secondary" onClick={() => setConfirming('close')}>
                    Close report
                  </Button>
                )}
                {isOwner && (
                  <Button variant="danger" onClick={() => setConfirming('delete')}>
                    Delete
                  </Button>
                )}
              </div>
            </section>
          )}
        </aside>
      </div>

      <ConfirmDialog
        open={confirming !== null}
        {...(CONFIRMATIONS[confirming] ?? CONFIRMATIONS.close)}
        loading={working}
        onConfirm={handleConfirm}
        onCancel={() => !working && setConfirming(null)}
      />
    </div>
  )
}
