import { Outlet } from 'react-router-dom'

import { useFetch } from '../hooks/useFetch.js'
import { getDashboard } from '../services/adminService.js'
import { ADMIN_CHANGED } from '../utils/constants.js'

/**
 * Shared by the admin pages. It loads the dashboard numbers once and hands them to the pages
 * below through the Outlet context, with a reload function to call after an admin changes something.
 * The admin links live in the main navbar (see MainLayout).
 */
export default function AdminLayout() {
  const { data: stats, error, reload } = useFetch(() => getDashboard(), [])

  const reloadStats = () => {
    reload()
    window.dispatchEvent(new Event(ADMIN_CHANGED)) // refresh the claims count in the navbar too
  }

  return <Outlet context={{ stats, statsError: error, reloadStats }} />
}
