import {useEffect, useState} from 'react'
import {NavLink, Outlet, useNavigate} from 'react-router-dom'
import {Button} from '../components/UI'
import {LogoutIcon} from '../components/Icons'
import type {AuthUserDetails} from '../contexts/AuthContext2'
import {useAuth} from '../contexts/AuthContext2'
import {useRootNavigation} from '../hooks/useRootNavigation'

const Layout = () => {
  const navigate = useNavigate()
  const {logout, getUser, isAuthenticated} = useAuth()
  const [userDetails, setUserDetails] = useState<AuthUserDetails | null>(null)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const {data: menuItems = [], isLoading: menuLoading} = useRootNavigation()

  useEffect(() => {
    const loadUserName = async () => {
      if (isAuthenticated) {
        try {
          return await getUser()
        } catch (error) {
          throw new Error("Error loading user name: ", {cause: error})
        }
      } else {
        throw new Error("No user is authenticated")
      }
    }

    loadUserName().then(setUserDetails)
  }, [isAuthenticated, getUser]) // Note: getUser is stable due to useCallback, but kept in deps for clarity

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  const handleUserNameClick = () => {
    if (userDetails) {
      navigate(`members/${userDetails.id}`)
    }
  }


  return (
      <div className="flex flex-col h-screen bg-white dark:bg-gray-900">
        {/* Header */}
        <header className="fixed top-0 left-0 right-0 bg-gray-800 dark:bg-gray-900 text-white shadow-md z-40">
          <div className="flex items-center justify-between h-16 px-6">
            {/* Logo/Title */}
            <div className="flex items-center gap-4">
              <button
                  onClick={() => setSidebarOpen(!sidebarOpen)}
                  className="text-white hover:bg-gray-700 dark:hover:bg-gray-800 p-2 rounded transition-colors"
              >
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16"/>
                </svg>
              </button>
              <h1 className="text-lg font-semibold"><NavLink to={"/"}>Klabis - Členská sekce</NavLink></h1>
            </div>

            {/* Right side: User info and logout */}
            <div className="flex items-center gap-4">
              {userDetails && (
                  <button
                      onClick={handleUserNameClick}
                      className="px-3 py-2 text-sm hover:bg-gray-700 dark:hover:bg-gray-800 rounded transition-colors"
                  >
                    {userDetails.firstName} {userDetails.lastName} [{userDetails.registrationNumber}]
                  </button>
              )}
              <Button
                  variant="ghost"
                  size="sm"
                  onClick={handleLogout}
                  endIcon={<LogoutIcon size={20}/>}
              >
                Odhlásit
              </Button>
            </div>
          </div>
        </header>

        {/* Sidebar overlay */}
        {sidebarOpen && (
            <div
                className="fixed inset-0 bg-black bg-opacity-50 z-20"
                onClick={() => setSidebarOpen(false)}
            />
        )}

        {/* Sidebar */}
        <aside
            className={`fixed left-0 top-16 h-[calc(100vh-4rem)] w-64 bg-gray-50 dark:bg-gray-800 border-r border-gray-300 dark:border-gray-700 shadow-lg transform transition-transform duration-300 ease-in-out z-30 ${
                sidebarOpen ? 'translate-x-0' : '-translate-x-full'
            }`}
        >
          <nav className="flex flex-col p-6 gap-4">
            {menuLoading ? (
                <div className="text-gray-500 dark:text-gray-400 text-sm">Loading menu...</div>
            ) : menuItems.length > 0 ? (
                menuItems.map((item) => (
                    <NavLink
                        key={item.rel}
                        to={item.href}
                        onClick={() => setSidebarOpen(false)}
                        className="px-4 py-2 text-gray-700 dark:text-gray-200 font-medium hover:bg-gray-200 dark:hover:bg-gray-700 rounded transition-colors"
                    >
                      {item.label}
                    </NavLink>
                ))
            ) : (
                <div className="text-gray-500 dark:text-gray-400 text-sm">No menu items available</div>
            )}
          </nav>
        </aside>

        {/* Main content */}
        <main className="flex-1 pt-16 px-6 py-6 overflow-auto">
          <Outlet/>
        </main>
      </div>
  )
}

export default Layout