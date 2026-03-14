import { ReactNode, useState, useEffect, useCallback } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import {
  DashboardIcon,
  BuildingIcon,
  UsersIcon,
  ClockIcon,
  DocumentIcon,
  MenuIcon,
  ChevronDownIcon,
} from './icons'

const LG = 1024

function useIsDesktop() {
  const [isDesktop, setIsDesktop] = useState(
    () => typeof window !== 'undefined' && window.innerWidth >= LG
  )
  useEffect(() => {
    const h = () => setIsDesktop(window.innerWidth >= LG)
    window.addEventListener('resize', h)
    return () => window.removeEventListener('resize', h)
  }, [])
  return isDesktop
}

const NAV = [
  { name: 'Dashboard', href: '/dashboard', icon: DashboardIcon },
  { name: 'Empresas', href: '/companies', icon: BuildingIcon },
  { name: 'Funcionários', href: '/employees', icon: UsersIcon },
  { name: 'Fechamento', href: '/timesheet', icon: DocumentIcon },
] as const

interface LayoutProps {
  children: ReactNode
}

export default function Layout({ children }: LayoutProps) {
  const { user, logout } = useAuth()
  const location = useLocation()
  const isDesktop = useIsDesktop()

  const [sidebarOpen, setSidebarOpen] = useState(
    () => typeof window !== 'undefined' && window.innerWidth >= LG
  )
  const [userMenuOpen, setUserMenuOpen] = useState(false)

  useEffect(() => {
    if (!isDesktop) setSidebarOpen(false)
  }, [isDesktop])

  const toggleSidebar = useCallback(() => setSidebarOpen((v) => !v), [])
  const closeSidebar = useCallback(() => setSidebarOpen(false), [])

  const currentTitle = NAV.find((n) => n.href === location.pathname)?.name ?? 'Dashboard'

  return (
    <div className="flex min-h-screen bg-gray-50">
      {sidebarOpen && !isDesktop && (
        <div
          className="fixed inset-0 z-40 bg-black/50"
          aria-hidden
          onClick={closeSidebar}
        />
      )}

      <aside
        className={
          'flex-shrink-0 border-r border-gray-200 bg-white transition-[width,transform] duration-300 ease-out ' +
          (isDesktop
            ? 'relative overflow-hidden ' + (sidebarOpen ? 'w-72' : 'w-0')
            : 'fixed inset-y-0 left-0 z-50 w-72 -translate-x-full transform ' +
              (sidebarOpen ? 'translate-x-0' : ''))
        }
      >
        <div className="flex h-full w-72 flex-col">
          <div className="flex h-16 items-center border-b border-gray-200 px-4">
            <div className="flex min-w-0 flex-1 items-center gap-3">
              <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-lg bg-gradient-to-br from-blue-600 to-blue-700">
                <ClockIcon className="h-6 w-6 text-white" />
              </div>
              <div className="min-w-0">
                <h1 className="truncate text-lg font-bold text-gray-900">RH System</h1>
                <p className="truncate text-xs text-gray-500">Gestão de Ponto</p>
              </div>
            </div>
          </div>

          <nav className="flex-1 space-y-1 overflow-y-auto px-3 py-4">
            {NAV.map((item) => {
              const Icon = item.icon
              const active = location.pathname === item.href
              return (
                <Link
                  key={item.href}
                  to={item.href}
                  onClick={() => !isDesktop && closeSidebar()}
                  className={
                    'flex items-center rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ' +
                    (active
                      ? 'bg-blue-50 text-blue-700 shadow-sm'
                      : 'text-gray-700 hover:bg-gray-50 hover:text-gray-900')
                  }
                >
                  <Icon
                    className={
                      'mr-3 h-5 w-5 flex-shrink-0 ' +
                      (active ? 'text-blue-600' : 'text-gray-400')
                    }
                  />
                  <span className="truncate">{item.name}</span>
                  {active && (
                    <span className="ml-auto h-1.5 w-1.5 flex-shrink-0 rounded-full bg-blue-600" />
                  )}
                </Link>
              )
            })}
          </nav>

          <div className="border-t border-gray-200 p-4">
            <div className="relative">
              <button
                type="button"
                onClick={() => setUserMenuOpen((v) => !v)}
                className="flex w-full items-center justify-between rounded-lg px-3 py-2 hover:bg-gray-50"
              >
                <div className="flex min-w-0 flex-1 items-center gap-3">
                  <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-blue-500 to-purple-600">
                    <span className="text-sm font-semibold text-white">
                      {(user?.name || 'U').charAt(0).toUpperCase()}
                    </span>
                  </div>
                  <div className="min-w-0 flex-1 text-left">
                    <p className="truncate text-sm font-medium text-gray-900">{user?.name}</p>
                    <p className="truncate text-xs text-gray-500">{user?.email}</p>
                  </div>
                </div>
                <ChevronDownIcon
                  className={
                    'h-4 w-4 flex-shrink-0 text-gray-400 transition-transform ' +
                    (userMenuOpen ? 'rotate-180' : '')
                  }
                />
              </button>
              {userMenuOpen && (
                <div className="absolute bottom-full left-0 right-0 mb-2 rounded-lg border border-gray-200 bg-white py-1 shadow-lg animate-fade-in">
                  <button
                    type="button"
                    onClick={() => {
                      logout()
                      setUserMenuOpen(false)
                    }}
                    className="w-full px-4 py-2 text-left text-sm text-red-600 hover:bg-red-50"
                  >
                    Sair
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      </aside>

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex h-16 items-center gap-4 border-b border-gray-200 bg-white px-4 shadow-sm lg:px-8">
          <button
            type="button"
            onClick={toggleSidebar}
            className="rounded-lg p-2 text-gray-500 hover:bg-gray-100 hover:text-gray-700"
            aria-expanded={sidebarOpen}
            aria-label={sidebarOpen ? 'Fechar menu' : 'Abrir menu'}
          >
            <MenuIcon className="h-6 w-6" />
          </button>
          <h2 className="text-lg font-semibold text-gray-900">{currentTitle}</h2>
        </header>

        <main className="flex-1 overflow-auto p-4 lg:p-8">
          <div className="mx-auto max-w-7xl">{children}</div>
        </main>
      </div>
    </div>
  )
}
