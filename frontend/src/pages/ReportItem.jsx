import { useNavigate } from 'react-router-dom'

import ItemForm from '../components/ItemForm.jsx'
import PageHeader from '../components/PageHeader.jsx'
import { useNotification } from '../hooks/useNotification.js'
import { createItem } from '../services/itemService.js'
import { usePageTitle } from '../hooks/usePageTitle.js'

export default function ReportItem() {
  usePageTitle('Report an item')
  const navigate = useNavigate()
  const notify = useNotification()

  const handleCreate = async (data) => {
    const item = await createItem(data)
    notify.success('Report created')
    navigate(`/items/${item.id}`)
  }

  return (
    <div className="mx-auto w-full max-w-4xl">
      <PageHeader title="Report an item" subtitle="Tell the campus what you lost or found." />
      <ItemForm submitLabel="Submit report" onSubmit={handleCreate} onCancel={() => navigate(-1)} />
    </div>
  )
}
