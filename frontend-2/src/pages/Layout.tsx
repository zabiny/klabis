import {useEffect, useState} from 'react'
import {Outlet, useNavigate} from 'react-router-dom'
import {Button} from '../components/UI'
import {LogoutIcon} from '../components/Icons'
import type {AuthUserDetails} from '../contexts/AuthContext2'
import {useAuth} from '../contexts/AuthContext2'

const Layout = () => {
  const navigate = useNavigate()
  const {logout, getUser, isAuthenticated} = useAuth()
  const [userDetails, setUserDetails] = useState<AuthUserDetails | null>(null)

  useEffect(() => {
    const loadUserName = async () => {
      if (isAuthenticated) {
        try {
          const user = await getUser()
          setUserDetails(user)
        } catch (error) {
          console.error('Error loading user name:', error)
        }
      }
    }

    loadUserName()
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
            <h1 className="text-lg font-semibold">Klabis - Členská sekce</h1>

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

        {/* Main content */}
        <main className="flex-1 pt-16 px-6 py-6 overflow-auto">
          <Outlet/>
        </main>
      </div>
  )
}

export default Layout