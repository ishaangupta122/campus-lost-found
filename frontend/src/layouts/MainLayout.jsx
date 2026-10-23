import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { LogOut, Menu, Search, X } from 'lucide-react'

import Button from '../components/Button.jsx'
import { useAuth } from '../hooks/useAuth.js'
import { useFetch } from '../hooks/useFetch.js'
import { getDashboard } from '../services/adminService.js'
import { ADMIN_CHANGED } from '../utils/constants.js'

// Links are plain text; the current page gets maroon text and (on desktop) a small bar underneath.
const NAV_LINK =
  'relative block border-b border-line px-1 py-3.5 text-lg font-medium text-ink-soft hover:text-ink hover:no-underline md:border-0 md:py-5 md:text-sm md:after:absolute md:after:inset-x-1 md:after:bottom-0 md:after:h-0.5 md:after:rounded-full'
const NAV_ACTIVE = 'text-primary hover:text-primary md:after:bg-primary'
const navLinkClass = ({ isActive }) => `${NAV_LINK} ${isActive ? NAV_ACTIVE : ''}`

// Admin links; "Claims" shows how many are waiting (reloaded when the page changes or an admin acts).
function AdminNavLinks() {
  const { pathname } = useLocation()
  const { data: stats, reload } = useFetch(() => getDashboard(), [pathname])

  // Admin pages fire this after approving, rejecting or closing something.
  useEffect(() => {
    window.addEventListener(ADMIN_CHANGED, reload)
    return () => window.removeEventListener(ADMIN_CHANGED, reload)
  })

  return (
    <>
      <li>
        <NavLink to="/admin" end className={navLinkClass}>
          Dashboard
        </NavLink>
      </li>
      <li>
        <NavLink to="/admin/claims" className={navLinkClass}>
          <span className="inline-flex items-center gap-1.5">
            Claims
            {stats?.pendingClaims > 0 && (
              <span className="inline-flex min-w-5 items-center justify-center rounded-full bg-primary px-1.5 text-xs text-on-primary">
                {stats.pendingClaims}
              </span>
            )}
          </span>
        </NavLink>
      </li>
      <li>
        <NavLink to="/admin/items" className={navLinkClass}>
          Items
        </NavLink>
      </li>
      <li>
        <NavLink to="/admin/users" className={navLinkClass}>
          Users
        </NavLink>
      </li>
    </>
  )
}

// The frame around every page: top navigation and page content (Outlet).
export default function MainLayout() {
  const { user, isAuthenticated, isAdmin, isStudent, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()

  // Close the mobile menu whenever we move to another page.
  useEffect(() => setMenuOpen(false), [location.pathname])

  // While the full-page mobile menu is open: the page behind it does not scroll, and Escape closes it.
  useEffect(() => {
    if (!menuOpen) return undefined
    const onKey = (event) => event.key === 'Escape' && setMenuOpen(false)
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', onKey)
    return () => {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', onKey)
    }
  }, [menuOpen])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="absolute -top-24 left-2 z-200 rounded-control bg-primary px-4 py-2 text-on-primary focus:top-2">
        Skip to content
      </a>

      <header className="sticky top-0 z-50 border-b border-line bg-card">
        <div className="mx-auto flex min-h-16 w-full max-w-280 items-center justify-between px-4 md:px-6">
          <Link to="/" className="inline-flex shrink-0 items-center gap-2.5 text-ink hover:no-underline">
            <span className="inline-flex size-9 items-center justify-center rounded-control bg-primary text-on-primary">
              <Search size={17} aria-hidden="true" />
            </span>
            <span className="flex flex-col leading-none">
              <span className="text-[0.625rem] font-medium tracking-[0.28em] text-ink-soft uppercase">Campus</span>
              <span className="font-display text-xl leading-none font-semibold tracking-tight whitespace-nowrap">
                Lost <span className="font-normal text-primary italic">&amp;</span> Found
              </span>
            </span>
          </Link>

          <button
            type="button"
            className="inline-flex cursor-pointer rounded-control p-2 text-ink hover:bg-sand md:hidden"
            onClick={() => setMenuOpen((open) => !open)}
            aria-expanded={menuOpen}
            aria-controls="main-nav"
            aria-label="Toggle navigation"
          >
            {menuOpen ? <X size={22} aria-hidden="true" /> : <Menu size={22} aria-hidden="true" />}
          </button>

          {/* Mobile: a full-page menu under the bar. Desktop (md and up): one row. */}
          <nav
            id="main-nav"
            aria-label="Main"
            className={`${menuOpen ? 'flex' : 'hidden'} fixed inset-x-0 top-16 bottom-0 z-40 flex-col justify-between gap-6 overflow-y-auto bg-card px-5 pt-2 pb-6 md:static md:flex md:flex-1 md:flex-row md:items-center md:justify-between md:gap-3 md:overflow-visible md:bg-transparent md:p-0 md:pl-10`}
          >
            <ul className="flex flex-col md:flex-row md:gap-5">
              <li>
                <NavLink to="/" end className={navLinkClass}>
                  Browse
                </NavLink>
              </li>
              {isStudent && (
                <>
                  <li>
                    <NavLink to="/report" className={navLinkClass}>
                      Report item
                    </NavLink>
                  </li>
                  <li>
                    <NavLink to="/my-items" className={navLinkClass}>
                      My items
                    </NavLink>
                  </li>
                  <li>
                    <NavLink to="/my-claims" className={navLinkClass}>
                      My claims
                    </NavLink>
                  </li>
                </>
              )}
              {isAdmin && <AdminNavLinks />}
            </ul>

            <div className="flex flex-col gap-3 md:flex-row md:items-center md:gap-2">
              {isAuthenticated ? (
                <>
                  <Link
                    to="/profile"
                    title="Your profile"
                    className="inline-flex items-center gap-3 rounded-control text-base font-medium text-ink hover:text-primary hover:no-underline md:gap-2 md:text-sm"
                  >
                    <span
                      aria-hidden="true"
                      className="inline-flex size-9 items-center justify-center rounded-full bg-primary-soft text-sm font-semibold text-primary md:size-7 md:text-xs"
                    >
                      {user.name.trim().charAt(0).toUpperCase()}
                    </span>
                    {user.name}
                  </Link>
                  <Button variant="secondary" className="w-full py-3! md:w-auto md:border-transparent md:bg-transparent md:py-1!" onClick={handleLogout}>
                    <LogOut size={16} aria-hidden="true" /> Log out
                  </Button>
                </>
              ) : (
                <>
                  <Button to="/login" variant="secondary" className="w-full py-3! md:w-auto md:border-transparent md:bg-transparent md:py-1!">
                    Log in
                  </Button>
                  <Button to="/register" className="w-full py-3! md:w-auto md:py-1!">
                    Sign up
                  </Button>
                </>
              )}
            </div>
          </nav>
        </div>
      </header>

      <main id="main" className="mx-auto w-full max-w-280 flex-1 px-4 pt-8 pb-10 md:px-6">
        <Outlet />
      </main>
    </div>
  )
}
