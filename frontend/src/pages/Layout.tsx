import {useEffect, useState} from 'react'
import {NavLink, Outlet, useNavigate} from 'react-router-dom'
import {Alert, Button, Spinner, Toast} from '../components/UI'
import {LogoutIcon} from '../components/UI/icons'
import {ThemeToggle} from '../components/ThemeToggle/ThemeToggle'
import {AdminToggle} from '../components/AdminToggle/AdminToggle'
import type {AuthUserDetails} from '../contexts/AuthContext2'
import {useAuth} from '../contexts/AuthContext2'
import {useRootNavigation} from '../hooks/useRootNavigation'
import {HalFormsPageLayout} from "../components/HalNavigator2/HalFormsPageLayout.tsx"
import {HalFormProvider} from '../contexts/HalFormContext.tsx'
import {ToastProvider, useToast} from '../contexts/ToastContext.tsx'

const LayoutToasts = () => {
    const {toasts, removeToast} = useToast();
    if (toasts.length === 0) return null;
    return (
        <div className="fixed top-20 right-4 z-[60] flex flex-col gap-2 max-w-sm pointer-events-none">
            {toasts.map(toast => (
                <div key={toast.id} className="pointer-events-auto">
                    <Toast toast={toast} onClose={removeToast}/>
                </div>
            ))}
        </div>
    );
};

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
        if (isAuthenticated) {
            setUserDetails(getUser())
        } else {
            setUserDetails(null)
        }
    }, [isAuthenticated, getUser])

    const handleLogout = () => {
        logout()
    }

    const handleUserNameClick = () => {
        if (userDetails?.memberId) {
            navigate(`members/${userDetails.memberId}`)
        }
    }

    return (
        <div className="flex flex-col min-h-screen bg-slate-100 dark:bg-zinc-950">
            {/* ==========================================
                PREMIUM HEADER - ELEVATED, GLASS EFFECT
                ========================================== */}
            <header className="fixed top-0 left-0 right-0 z-50 bg-white dark:bg-zinc-900 border-b border-zinc-200 dark:border-zinc-800">
                <div className="flex items-center justify-between h-16 px-4 sm:px-6 lg:px-6">
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
                            {/* Logo icon - solid blue square with compass */}
                            <div className="flex-shrink-0 w-9 h-9 rounded-lg bg-blue-600 flex items-center justify-center">
                                <svg className="w-5 h-5 text-white" xmlns="http://www.w3.org/2000/svg"
                                     fill="none" stroke="currentColor" strokeWidth="2"
                                     strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24">
                                    <circle cx="12" cy="12" r="10"/>
                                    <polygon points="16.24 7.76 14.12 14.12 7.76 16.24 9.88 9.88 16.24 7.76"/>
                                </svg>
                            </div>

                            <h1 className="text-lg font-bold text-zinc-900 dark:text-zinc-50">
                                Klabis
                            </h1>
                        </NavLink>
                    </div>

                    {/* Right side: User info, theme, admin, logout */}
                    <div className="flex items-center gap-2 sm:gap-3">
                        {/* User name/info */}
                        {userDetails && (
                            userDetails.firstName && userDetails.lastName ? (
                                userDetails.memberId ? (
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        onClick={handleUserNameClick}
                                        className="hidden sm:inline-flex"
                                        title="Zobrazit detail člena"
                                    >
                                        {userDetails.firstName} {userDetails.lastName}
                                        <span className="ml-2 font-normal text-zinc-400 dark:text-zinc-500">[{userDetails.userName}]</span>
                                    </Button>
                                ) : (
                                    <div className="hidden sm:flex px-3 py-2 text-sm text-zinc-500">
                                        <span className="font-medium">{userDetails.firstName} {userDetails.lastName}</span>
                                        <span className="ml-2">[{userDetails.userName}]</span>
                                    </div>
                                )
                            ) : (
                                <span className="hidden sm:inline-flex px-3 py-2 text-sm text-zinc-500">
                                    [{userDetails.userName}]
                                </span>
                            )
                        )}

                        {/* Theme toggle */}
                        <ThemeToggle className="hidden sm:block border border-zinc-200 dark:border-zinc-700 bg-slate-50 dark:bg-zinc-800" />

                        {/* Admin mode toggle */}
                        <AdminToggle />

                        {/* Logout button */}
                        <Button
                            variant="danger-ghost"
                            size="sm"
                            onClick={handleLogout}
                            title="Odhlásit"
                            className="hidden sm:inline-flex"
                            startIcon={<LogoutIcon size={16}/>}
                        >
                            <span className="hidden lg:inline">Odhlásit</span>
                        </Button>

                        {/* Mobile menu - show all in dropdown */}
                        <div className="sm:hidden flex items-center gap-2">
                            <Button
                                variant="danger-ghost"
                                size="sm"
                                onClick={handleLogout}
                                aria-label="Odhlásit"
                            >
                                <LogoutIcon size={18}/>
                            </Button>
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
                className={`fixed left-0 top-16 h-[calc(100vh-4rem)] w-60 bg-white dark:bg-zinc-900 border-r border-zinc-200 dark:border-zinc-800 shadow-sm transform transition-all duration-300 ease-in-out z-30 flex flex-col ${
                    isLargeScreen ? 'translate-x-0' : (sidebarOpen ? 'translate-x-0' : '-translate-x-full')
                }`}
            >
                <nav className="flex flex-col px-3 py-4 gap-1 overflow-y-auto flex-1">
                    <p className="px-2 pb-2 text-[11px] font-semibold text-zinc-400 dark:text-zinc-500 uppercase tracking-wide">Navigace</p>
                    {menuLoading ? (
                        <div className="flex items-center gap-3 px-4 py-3 text-text-tertiary text-sm">
                            <Spinner size="sm"/>
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
                                    const base = "flex items-center gap-3 px-3 rounded-lg text-sm transition-all duration-fast h-10"
                                    const active = "bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-400 font-semibold"
                                    const inactive = "text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-50 dark:hover:bg-zinc-800 font-medium"
                                    return `${base} ${isActive ? active : inactive}`
                                }}
                                children={() => (
                                    <span className="flex-1">{item.label}</span>
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
                <div className="p-4 border-t border-zinc-200 dark:border-zinc-800">
                    <div className="text-xs">
                        <div className="font-semibold text-zinc-500 dark:text-zinc-400 mb-0.5">Klabis Club Manager</div>
                        <div className="text-zinc-400 dark:text-zinc-500">Verze 1.0.0</div>
                    </div>
                </div>
            </aside>

            {/* ==========================================
                MAIN CONTENT AREA
                ========================================== */}
            <ToastProvider>
                <LayoutToasts />
                <main className="flex-1 pt-20 px-4 sm:px-6 lg:px-8 py-6 lg:pl-60 overflow-auto bg-slate-100 dark:bg-zinc-950 min-h-screen">
                    <HalFormProvider>
                        <HalFormsPageLayout>
                            <Outlet />
                        </HalFormsPageLayout>
                    </HalFormProvider>
                </main>
            </ToastProvider>
        </div>
    )
}

export default Layout
