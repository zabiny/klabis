import {useEffect, useState} from 'react'
import {NavLink, Outlet, useNavigate} from 'react-router-dom'
import {Alert} from '../components/UI'
import {LogoutIcon} from '../components/UI/icons'
import {ThemeToggle} from '../components/ThemeToggle/ThemeToggle'
import {AdminToggle} from '../components/AdminToggle/AdminToggle'
import type {AuthUserDetails} from '../contexts/AuthContext2'
import {useAuth} from '../contexts/AuthContext2'
import {useRootNavigation} from '../hooks/useRootNavigation'
import {HalFormsPageLayout} from "../components/HalNavigator2/HalFormsPageLayout.tsx"
import {HalFormProvider} from '../contexts/HalFormContext.tsx'

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
                    const user = await getUser()
                    setUserDetails(user)
                } catch (error) {
                    console.error("Error loading user name: ", error)
                    setUserDetails(null)
                }
            } else {
                setUserDetails(null)
            }
        }

        loadUserName()
    }, [isAuthenticated, getUser])

    const handleLogout = () => {
        logout()
        navigate('/login')
    }

    const handleUserNameClick = () => {
        if (userDetails?.isMember && userDetails.firstName && userDetails.lastName) {
            navigate(`members/${userDetails.id}`)
        }
    }

    return (
        <div className="flex flex-col min-h-screen bg-bg-base">
            {/* ==========================================
                PREMIUM HEADER - ELEVATED, GLASS EFFECT
                ========================================== */}
            <header className="fixed top-0 left-0 right-0 z-50 glass border-b border-border-subtle">
                <div className="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
                    {/* Left side: Logo + mobile menu toggle */}
                    <div className="flex items-center gap-3 sm:gap-4">
                        {/* Toggle button - hide on lg screens */}
                        {!isLargeScreen && (
                            <button
                                onClick={() => setSidebarOpen(!sidebarOpen)}
                                className="p-2.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-bg-subtle transition-all duration-fast focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
                                aria-label="Toggle menu"
                                type="button"
                            >
                                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={1.5}>
                                    <path strokeLinecap="round" strokeLinejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
                                </svg>
                            </button>
                        )}

                        {/* Logo/Title */}
                        <NavLink to="/" className="flex items-center gap-3 group">
                            {/* Logo icon */}
                            <div className="flex-shrink-0 w-10 h-10 rounded-lg bg-gradient-to-br from-primary to-secondary flex items-center justify-center shadow-md group-hover:shadow-lg transition-all duration-base">
                                <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
                                    <path d="M12 2L2 7l10 5 10 10l-10-5-5 10 5-5-5 10 10 5 5-5-5 5 10z"/>
                                </svg>
                            </div>

                            <h1 className="text-lg sm:text-xl font-display font-bold text-text-primary group-hover:text-primary transition-colors duration-fast">
                                Klabis
                            </h1>
                        </NavLink>
                    </div>

                    {/* Right side: User info, theme, admin, logout */}
                    <div className="flex items-center gap-2 sm:gap-3">
                        {/* User name/info */}
                        {userDetails && (
                            userDetails.firstName && userDetails.lastName ? (
                                userDetails.isMember ? (
                                    <button
                                        onClick={handleUserNameClick}
                                        className="hidden sm:flex px-3 py-2 text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-bg-subtle rounded-lg transition-all duration-fast"
                                        title="Zobrazit detail člena"
                                    >
                                        <span className="text-text-primary font-semibold">{userDetails.firstName} {userDetails.lastName}</span>
                                        <span className="text-text-tertiary ml-2">[{userDetails.userName}]</span>
                                    </button>
                                ) : (
                                    <div className="hidden sm:flex px-3 py-2 text-sm text-text-secondary">
                                        <span className="font-medium text-text-primary">{userDetails.firstName} {userDetails.lastName}</span>
                                        <span className="text-text-tertiary ml-2">[{userDetails.userName}]</span>
                                    </div>
                                )
                            ) : (
                                <span className="hidden sm:inline-flex px-3 py-2 text-sm text-text-secondary">
                                    [{userDetails.userName}]
                                </span>
                            )
                        )}

                        {/* Theme toggle */}
                        <ThemeToggle className="hidden sm:block" />

                        {/* Admin mode toggle */}
                        <AdminToggle />

                        {/* Logout button */}
                        <button
                            onClick={handleLogout}
                            className="hidden sm:inline-flex px-4 py-2 text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-bg-subtle rounded-lg transition-all duration-fast focus:outline-none focus:ring-2 focus:ring-error focus:ring-offset-2"
                            title="Odhlásit"
                            type="button"
                        >
                            <LogoutIcon size={18} className="mr-2"/>
                            <span className="hidden lg:inline">Odhlásit</span>
                        </button>

                        {/* Mobile menu - show all in dropdown */}
                        <div className="sm:hidden flex items-center gap-2">
                            <button
                                onClick={handleLogout}
                                className="p-2 rounded-lg text-text-secondary hover:text-text-primary hover:bg-bg-subtle transition-all duration-fast focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
                                aria-label="Odhlásit"
                                type="button"
                            >
                                <LogoutIcon size={18}/>
                            </button>
                        </div>
                    </div>
                </div>
            </header>

            {/* ==========================================
                SIDEBAR OVERLAY (mobile only)
                ========================================== */}
            {sidebarOpen && !isLargeScreen && (
                <div
                    className="fixed inset-0 bg-black/60 backdrop-blur-sm z-20 transition-opacity duration-base"
                    onClick={() => setSidebarOpen(false)}
                    data-testid="sidebar-overlay"
                />
            )}

            {/* ==========================================
                SIDEBAR - NAVIGAČNÍ MENU
                ========================================== */}
            <aside
                className={`fixed left-0 top-16 h-[calc(100vh-4rem)] w-72 bg-bg-elevated border-r border-border-subtle shadow-lg transform transition-all duration-300 ease-in-out z-30 ${
                    isLargeScreen ? 'translate-x-0' : (sidebarOpen ? 'translate-x-0' : '-translate-x-full')
                }`}
            >
                <nav className="flex flex-col p-3 gap-1 overflow-y-auto">
                    {menuLoading ? (
                        <div className="flex items-center gap-3 px-4 py-3 text-text-tertiary text-sm">
                            <div className="w-5 h-5 rounded border-2 border-border-current border-t-transparent animate-spin" />
                            Načítání menu...
                        </div>
                    ) : menuError ? (
                        <Alert severity="error" className="m-3 text-sm">
                            Chyba při načítání menu: {menuError.message}
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
                                className={({isActive}: {isActive: boolean}) => {
                                    const base = "flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all duration-fast"
                                    const active = "bg-primary-subtle text-primary border-l-2 border-primary"
                                    const inactive = "text-text-secondary hover:text-text-primary hover:bg-bg-subtle border-l-2 border-transparent"
                                    return `${base} ${isActive ? active : inactive}`
                                }}
                                children={({isActive}: {isActive: boolean}) => (
                                    <>
                                        {/* Active indicator dot */}
                                        {isActive && (
                                            <div className="w-1.5 h-1.5 rounded-full bg-primary shadow-sm shadow-primary/50" />
                                        )}

                                        <span className="flex-1">{item.label}</span>

                                        {/* Arrow icon on hover/active */}
                                        <svg className="w-4 h-4 transition-transform duration-fast opacity-0 -translate-x-0.5 group-hover:opacity-100" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
                                            <path strokeLinecap="round" strokeLinejoin="round" d="M9 5l7 7-7 7" />
                                        </svg>
                                    </>
                                )}
                            />
                        ))
                    ) : (
                        <div className="text-text-tertiary text-sm px-4 py-3">
                            Žádné položky menu nejsou dostupné
                        </div>
                    )}
                </nav>

                {/* Sidebar footer - app info */}
                <div className="mt-auto p-4 border-t border-border-subtle">
                    <div className="text-xs text-text-tertiary">
                        <div className="font-medium text-text-secondary mb-1">Klabis Club Manager</div>
                        <div>Versi 1.0.0</div>
                    </div>
                </div>
            </aside>

            {/* ==========================================
                MAIN CONTENT AREA
                ========================================== */}
            <main className="flex-1 pt-20 px-4 sm:px-6 lg:px-8 py-6 lg:pl-80 overflow-auto">
                <HalFormProvider>
                    <HalFormsPageLayout>
                        <Outlet />
                    </HalFormsPageLayout>
                </HalFormProvider>
            </main>
        </div>
    )
}

export default Layout
