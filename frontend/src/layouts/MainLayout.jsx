import { useEffect, useState } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { LogOut, Menu, Search, X } from 'lucide-react'

import Button from '../components/Button.jsx'
import { useAuth } from '../hooks/useAuth.js'

// Links are plain text; the current page gets maroon text and (on desktop) a small bar underneath.
const NAV_LINK =
  'relative block px-1 py-2 text-sm font-medium text-ink-soft hover:text-ink hover:no-underline md:py-5 md:after:absolute md:after:inset-x-1 md:after:bottom-0 md:after:h-0.5 md:after:rounded-full'
const NAV_ACTIVE = 'text-primary hover:text-primary md:after:bg-primary'
const navLinkClass = ({ isActive }) => `${NAV_LINK} ${isActive ? NAV_ACTIVE : ''}`

// The frame around every page: top navigation, page content (Outlet) and footer.
export default function MainLayout() {
  const { user, isAuthenticated, isAdmin, isStudent, logout } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()

  // Close the mobile menu whenever we move to another page.
  useEffect(() => setMenuOpen(false), [location.pathname])

  const handleLogout = () => {
    logout()
    navigate('/')
  }

  return (
    <div className="flex min-h-screen flex-col">
      <a href="#main" className="absolute -top-24 left-2 z-200 rounded-control bg-primary px-4 py-2 text-on-primary focus:top-2">
        Skip to content
      </a>

      <header className="sticky top-0 z-50 bg-card shadow-card">
        <div className="mx-auto flex min-h-16 w-full max-w-280 items-center justify-between px-4 md:px-6">
          <Link to="/" className="inline-flex shrink-0 items-center gap-2.5 text-ink hover:no-underline">
            <span className="inline-flex size-9 items-center justify-center rounded-control bg-primary text-on-primary">
              <Search size={17} aria-hidden="true" />
            </span>
            <span className="flex flex-col leading-none">
              <span className="font-display text-xl font-semibold tracking-tight whitespace-nowrap">
                Lost <span className="font-normal text-primary italic">&amp;</span> Found
              </span>
              <span className="mt-1 text-[0.625rem] font-medium tracking-[0.28em] text-ink-soft uppercase">Campus</span>
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

          {/* Mobile: a dropdown under the bar. Desktop (md and up): one row. */}
          <nav
            id="main-nav"
            aria-label="Main"
            className={`${menuOpen ? 'flex' : 'hidden'} absolute inset-x-0 top-16 flex-col gap-3 bg-card p-4 shadow-card md:static md:shadow-none md:flex md:flex-1 md:flex-row md:items-center md:justify-between md:border-0 md:bg-transparent md:p-0 md:pl-10`}
          >
            <ul className="flex flex-col gap-1 md:flex-row md:gap-5">
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
              {isAdmin && (
                <li>
                  <NavLink to="/admin" className={navLinkClass}>
                    Admin
                  </NavLink>
                </li>
              )}
            </ul>

            <div className="flex items-center gap-2">
              {isAuthenticated ? (
                <>
                  <Link
                    to="/profile"
                    title="Your profile"
                    className="inline-flex items-center gap-2 rounded-control text-sm font-medium text-ink hover:text-primary hover:no-underline"
                  >
                    <span
                      aria-hidden="true"
                      className="inline-flex size-7 items-center justify-center rounded-full bg-primary-soft text-xs font-semibold text-primary"
                    >
                      {user.name.trim().charAt(0).toUpperCase()}
                    </span>
                    {user.name}
                  </Link>
                  <Button variant="ghost" size="sm" onClick={handleLogout}>
                    <LogOut size={16} aria-hidden="true" /> Log out
                  </Button>
                </>
              ) : (
                <>
                  <Button to="/login" variant="ghost" size="sm">
                    Log in
                  </Button>
                  <Button to="/register" size="sm">
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

      <footer className="py-4">
        <div className="mx-auto w-full max-w-280 px-4 text-sm text-ink-soft md:px-6">
          Campus Lost &amp; Found - college project
        </div>
      </footer>
    </div>
  )
}
