import {useEffect, useState} from 'react'
import {NavLink, Outlet, useNavigate} from 'react-router-dom'
import {Alert, Button} from '../components/UI'
import {LogoutIcon} from '../components/Icons'
import {ThemeToggle} from '../components/ThemeToggle/ThemeToggle'
import type {AuthUserDetails} from '../contexts/AuthContext2'
import {useAuth} from '../contexts/AuthContext2'
import {useRootNavigation} from '../hooks/useRootNavigation'
import {HalFormsPageLayout} from "../components/HalNavigator2/HalFormsPageLayout.tsx";

const Layout = () => {
  const navigate = useNavigate()
  const {logout, getUser, isAuthenticated} = useAuth()
  const [userDetails, setUserDetails] = useState<AuthUserDetails | null>(null)
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const [isLargeScreen, setIsLargeScreen] = useState(window.innerWidth >= 1024)
  const {data: menuItems = [], isLoading: menuLoading, error: menuError} = useRootNavigation()

  // Track screen size changes for responsive sidebar
  useEffect(() => {
    const handleResize = () => {
      setIsLargeScreen(window.innerWidth >= 1024)
    }
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

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
      <div className="flex flex-col h-screen bg-black">
        {/* Header */}
        <header
            className="fixed top-0 left-0 right-0 bg-surface-raised border-b border-border text-text-primary shadow-md z-40">
          <div className="flex items-center justify-between h-16 px-6">
            {/* Logo/Title */}
            <div className="flex items-center gap-4">
              {/* Toggle button - hide on lg screens */}
              {!isLargeScreen && (
                  <button
                      onClick={() => setSidebarOpen(!sidebarOpen)}
                      className="text-text-primary hover:bg-surface-base p-2 rounded-md transition-colors duration-base"
                  >
                    <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16"/>
                    </svg>
                  </button>
              )}
              <h1 className="text-lg font-semibold font-display"><NavLink to={"/"}
                                                                          className="text-text-primary hover:text-primary transition-colors">Klabis
                - Členská sekce</NavLink></h1>
            </div>

            {/* Right side: User info and logout */}
            <div className="flex items-center gap-4">
              {userDetails && (
                  <button
                      onClick={handleUserNameClick}
                      className="px-3 py-2 text-sm text-text-secondary hover:text-text-primary hover:bg-surface-base rounded-md transition-colors duration-base"
                  >
                    {userDetails.firstName} {userDetails.lastName} [{userDetails.registrationNumber}]
                  </button>
              )}
              <ThemeToggle/>
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

        {/* Sidebar overlay - only show on small screens */}
        {sidebarOpen && !isLargeScreen && (
            <div
                className="fixed inset-0 bg-black bg-opacity-60 z-20 transition-opacity duration-base"
                onClick={() => setSidebarOpen(false)}
                data-testid="sidebar-overlay"
            />
        )}

        {/* Sidebar - always visible on lg screens, drawer on small screens */}
        <aside
            className={`fixed left-0 top-16 h-[calc(100vh-4rem)] w-64 bg-surface-base border-r border-border shadow-lg transform transition-transform duration-300 ease-in-out z-30 ${
                isLargeScreen ? 'translate-x-0' : (sidebarOpen ? 'translate-x-0' : '-translate-x-full')
            }`}
        >
          <nav className="flex flex-col p-4 gap-2">
            {menuLoading ? (
                <div className="text-text-tertiary text-sm">Loading menu...</div>
            ) : menuError ? (
                <Alert severity="error" className="text-sm">
                  Failed to load menu: {menuError.message}
                </Alert>
            ) : menuItems.length > 0 ? (
                menuItems.map((item) => (
                    <NavLink
                        key={item.rel}
                        to={item.href}
                        onClick={() => {
                          // Only close sidebar on small screens when item is clicked
                          if (!isLargeScreen) {
                            setSidebarOpen(false)
                          }
                        }}
                        className={({isActive}) => `px-4 py-2 text-text-secondary font-medium hover:text-text-primary hover:bg-surface-raised rounded-md transition-all duration-base border-l-4 ${isActive ? 'border-l-primary bg-surface-raised text-primary' : 'border-l-transparent'}`}
                    >
                      {item.label}
                    </NavLink>
                ))
            ) : (
                <div className="text-text-tertiary text-sm">No menu items available</div>
            )}
          </nav>
        </aside>

        {/* Main content - add left padding on lg screens for sidebar with extra spacing */}
        <main className="flex-1 pt-20 px-6 py-6 overflow-auto lg:pl-72">
          <HalFormsPageLayout>
          <Outlet/>
          </HalFormsPageLayout>
        </main>
      </div>
  )
}

export default Layout