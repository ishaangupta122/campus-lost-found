import { Navigate, useNavigate, useParams } from 'react-router-dom'

import ItemForm from '../components/ItemForm.jsx'
import PageHeader from '../components/PageHeader.jsx'
import { ErrorMessage, LoadingState } from '../components/States.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useFetch } from '../hooks/useFetch.js'
import { useNotification } from '../hooks/useNotification.js'
import { getItem, updateItem } from '../services/itemService.js'
import { usePageTitle } from '../hooks/usePageTitle.js'

export default function EditItem() {
  usePageTitle('Edit report')
  const { id } = useParams()
  const { user } = useAuth()
  const navigate = useNavigate()
  const notify = useNotification()
  const { data: item, loading, error, reload } = useFetch(() => getItem(id), [id])

  if (loading && !item) return <LoadingState message="Loading item..." />
  if (error) return <ErrorMessage message={error} onRetry={reload} />
  if (!item) return null

  // Only the owner can edit, and only while the report is still active.
  const active = item.status === 'OPEN' || item.status === 'CLAIM_PENDING'
  if (item.reporterId !== user.id || !active) return <Navigate to={`/items/${item.id}`} replace />

  const handleUpdate = async (data) => {
    await updateItem(item.id, data)
    notify.success('Report updated')
    navigate(`/items/${item.id}`)
  }

  return (
    <div className="mx-auto w-full max-w-4xl">
      <PageHeader title="Edit report" subtitle="The type (lost or found) cannot be changed." />
      <ItemForm item={item} submitLabel="Save changes" onSubmit={handleUpdate} onCancel={() => navigate(-1)} />
    </div>
  )
}
