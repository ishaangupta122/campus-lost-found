import { useState } from 'react'
import { Plus } from 'lucide-react'

import Button from '../components/Button.jsx'
import ConfirmDialog from '../components/ConfirmDialog.jsx'
import ItemCard from '../components/ItemCard.jsx'
import PageHeader from '../components/PageHeader.jsx'
import { EmptyState, ErrorMessage, LoadingState } from '../components/States.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { useNotification } from '../hooks/useNotification.js'
import { getApiError } from '../services/api.js'
import { closeItem, deleteItem, getMyItems } from '../services/itemService.js'
import { usePageTitle } from '../hooks/usePageTitle.js'

export default function MyItems() {
  usePageTitle('My items')
  const notify = useNotification()
  const { data: items, loading, error, reload } = useFetch(() => getMyItems(), [])
  const [pending, setPending] = useState(null) // { action: 'close' | 'delete', item }
  const [working, setWorking] = useState(false)

  const handleConfirm = async () => {
    setWorking(true)
    try {
      if (pending.action === 'close') {
        await closeItem(pending.item.id)
        notify.success('Report closed')
      } else {
        await deleteItem(pending.item.id)
        notify.success('Report deleted')
      }
      reload()
    } catch (err) {
      notify.error(getApiError(err).message)
    } finally {
      setWorking(false)
      setPending(null)
    }
  }

  const closing = pending?.action === 'close'

  return (
    <>
      <PageHeader title="My items" subtitle="Everything you have reported.">
        <Button to="/report">
          <Plus size={16} aria-hidden="true" /> Report an item
        </Button>
      </PageHeader>

      {loading && !items && <LoadingState message="Loading your reports..." />}
      {error && <ErrorMessage message={error} onRetry={reload} />}
      {items && items.length === 0 && (
        <EmptyState
          title="You have not reported anything yet"
          message="Lost or found something on campus? Create a report."
          action={{ label: 'Report an item', to: '/report' }}
        />
      )}
      {items && items.length > 0 && (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((item) => {
            const active = item.status === 'OPEN' || item.status === 'CLAIM_PENDING'
            return (
              <ItemCard key={item.id} item={item}>
                {active && (
                  <>
                    <Button size="sm" variant="secondary" to={`/items/${item.id}/edit`}>
                      Edit
                    </Button>
                    <Button size="sm" variant="secondary" onClick={() => setPending({ action: 'close', item })}>
                      Close
                    </Button>
                  </>
                )}
                <Button size="sm" variant="ghost" onClick={() => setPending({ action: 'delete', item })}>
                  Delete
                </Button>
              </ItemCard>
            )
          })}
        </div>
      )}

      <ConfirmDialog
        open={pending !== null}
        title={closing ? 'Close this report?' : 'Delete this report?'}
        message={
          closing
            ? 'Waiting claims will be rejected and the report will no longer be listed.'
            : 'This cannot be undone. A report that has claims cannot be deleted; close it instead.'
        }
        confirmLabel={closing ? 'Close report' : 'Delete'}
        danger
        loading={working}
        onConfirm={handleConfirm}
        onCancel={() => !working && setPending(null)}
      />
    </>
  )
}
