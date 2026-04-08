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
import {labels} from '../localization'
import {Home, Calendar, Trophy, Users, UsersRound, Layers, Dumbbell, Heart, Tags} from 'lucide-react'
import type {LucideIcon} from 'lucide-react'

const navIcons: Record<string, LucideIcon> = {
    home: Home,
    calendar: Calendar,
    events: Trophy,
    members: Users,
    groups: UsersRound,
    'training-groups': Dumbbell,
    'family-groups': Heart,
    'category-presets': Tags,
}

const getNavIcon = (rel: string): LucideIcon => navIcons[rel] ?? Layers

const bottomNavClassName = ({isActive}: {isActive: boolean}) => {
    const base = "flex flex-col items-center justify-center flex-1 h-full gap-1 text-[11px] transition-colors duration-fast"
    const active = "text-blue-600 dark:text-blue-400 font-semibold"
    const inactive = "text-zinc-400 dark:text-zinc-500"
    return `${base} ${isActive ? active : inactive}`
}

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
    const [isLargeScreen, setIsLargeScreen] = useState(window.innerWidth >= 768)
    const {data: menuItems = [], isLoading: menuLoading, error: menuError} = useRootNavigation()
    const mainItems = menuItems.filter(item => item.section === 'main')
    const adminItems = menuItems.filter(item => item.section === 'admin')

    // Track screen size changes for responsive sidebar
    useEffect(() => {
        const handleResize = () => {
            setIsLargeScreen(window.innerWidth >= 768)
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
                    {/* Left side: Logo */}
                    <div className="flex items-center gap-3 sm:gap-4">
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
                                        title={labels.ui.showMemberDetails}
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
                        <ThemeToggle className="border border-zinc-200 dark:border-zinc-700 bg-slate-50 dark:bg-zinc-800" />

                        {/* Admin mode toggle */}
                        <AdminToggle />

                        {/* Logout button */}
                        <Button
                            variant="danger-ghost"
                            size="sm"
                            onClick={handleLogout}
                            title={labels.buttons.logout}
                            className="hidden sm:inline-flex"
                            startIcon={<LogoutIcon size={16}/>}
                        >
                            <span className="hidden lg:inline">{labels.buttons.logout}</span>
                        </Button>

                        {/* Mobile menu - show all in dropdown */}
                        <div className="sm:hidden flex items-center gap-2">
                            <Button
                                variant="danger-ghost"
                                size="sm"
                                onClick={handleLogout}
                                aria-label={labels.buttons.logout}
                            >
                                <LogoutIcon size={18}/>
                            </Button>
                        </div>
                    </div>
                </div>
            </header>

            {/* ==========================================
                BOTTOM NAVIGATION (mobile only)
                ========================================== */}
            {!isLargeScreen && (
                <nav
                    className="fixed bottom-0 left-0 right-0 z-50 bg-white dark:bg-zinc-900 border-t border-zinc-200 dark:border-zinc-800 shadow-[0_-1px_3px_rgba(0,0,0,0.05)] pb-[env(safe-area-inset-bottom)]"
                    aria-label={labels.ui.navigation}
                >
                    <div className="flex items-center justify-around h-16 px-2">
                        {menuLoading ? (
                            <div className="flex items-center justify-center w-full">
                                <Spinner size="sm"/>
                            </div>
                        ) : menuError ? (
                            <div className="text-xs text-red-500 px-2 text-center">
                                {labels.ui.menuLoadError}
                            </div>
                        ) : menuItems.length > 0 ? (
                            <>
                                <NavLink to="/" end className={bottomNavClassName}>
                                    <Home className="w-5 h-5" />
                                    <span>{labels.nav.home}</span>
                                </NavLink>
                                {mainItems.map((item) => {
                                    const Icon = getNavIcon(item.rel)
                                    return (
                                        <NavLink key={item.rel} to={item.href} className={bottomNavClassName}>
                                            <Icon className="w-5 h-5" />
                                            <span className="truncate max-w-[5rem]">{item.label}</span>
                                        </NavLink>
                                    )
                                })}
                            </>
                        ) : (
                            <div className="text-xs text-zinc-500 dark:text-zinc-400 px-2 text-center">
                                {labels.ui.noMenuAvailable}
                            </div>
                        )}
                    </div>
                </nav>
            )}

            {/* ==========================================
                SIDEBAR - NAVIGAČNÍ MENU (desktop only)
                ========================================== */}
            {isLargeScreen && (
                <aside className="fixed left-0 top-16 h-[calc(100vh-4rem)] w-60 bg-white dark:bg-zinc-900 border-r border-zinc-200 dark:border-zinc-800 shadow-sm z-30 flex flex-col">
                    <nav className="flex flex-col px-3 py-4 gap-1 overflow-y-auto flex-1">
                        <p className="px-2 pb-2 text-[11px] font-semibold text-zinc-400 dark:text-zinc-500 uppercase tracking-wide">{labels.ui.navigation}</p>
                        {menuLoading ? (
                            <div className="flex items-center gap-3 px-4 py-3 text-text-tertiary text-sm">
                                <Spinner size="sm"/>
                                {labels.errors.loadingMenu}
                            </div>
                        ) : menuError ? (
                            <Alert severity="error" className="m-3 text-sm">
                                {labels.ui.menuLoadError}: {menuError.message}
                            </Alert>
                        ) : menuItems.length > 0 ? (
                            <>
                                {mainItems.map((item) => {
                                    const Icon = getNavIcon(item.rel)
                                    return (
                                        <NavLink
                                            key={item.rel}
                                            to={item.href}
                                            className={({isActive}: {isActive: boolean}) => {
                                                const base = "flex items-center gap-3 px-3 rounded-lg text-sm transition-all duration-fast h-10"
                                                const active = "bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-400 font-semibold"
                                                const inactive = "text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-50 dark:hover:bg-zinc-800 font-medium"
                                                return `${base} ${isActive ? active : inactive}`
                                            }}
                                        >
                                            <Icon className="w-4 h-4" />
                                            <span className="flex-1">{item.label}</span>
                                        </NavLink>
                                    )
                                })}
                                {adminItems.length > 0 && (
                                    <>
                                        <p className="px-2 pt-4 pb-2 text-[11px] font-semibold text-zinc-400 dark:text-zinc-500 uppercase tracking-wide">{labels.ui.navAdminSection}</p>
                                        {adminItems.map((item) => {
                                            const Icon = getNavIcon(item.rel)
                                            return (
                                                <NavLink
                                                    key={item.rel}
                                                    to={item.href}
                                                    className={({isActive}: {isActive: boolean}) => {
                                                        const base = "flex items-center gap-3 px-3 rounded-lg text-sm transition-all duration-fast h-10"
                                                        const active = "bg-blue-50 dark:bg-blue-950 text-blue-600 dark:text-blue-400 font-semibold"
                                                        const inactive = "text-zinc-500 dark:text-zinc-400 hover:text-zinc-900 dark:hover:text-zinc-100 hover:bg-zinc-50 dark:hover:bg-zinc-800 font-medium"
                                                        return `${base} ${isActive ? active : inactive}`
                                                    }}
                                                >
                                                    <Icon className="w-4 h-4" />
                                                    <span className="flex-1">{item.label}</span>
                                                </NavLink>
                                            )
                                        })}
                                    </>
                                )}
                            </>
                        ) : (
                            <div className="text-text-tertiary text-sm px-4 py-3">
                                {labels.ui.noMenuAvailable}
                            </div>
                        )}
                    </nav>

                    <div className="p-4 border-t border-zinc-200 dark:border-zinc-800">
                        <div className="text-xs">
                            <div className="font-semibold text-zinc-500 dark:text-zinc-400 mb-0.5">{labels.ui.appName}</div>
                            <div className="text-zinc-400 dark:text-zinc-500">{labels.ui.appVersion}</div>
                        </div>
                    </div>
                </aside>
            )}

            {/* ==========================================
                MAIN CONTENT AREA
                ========================================== */}
            <ToastProvider>
                <LayoutToasts />
                {/* lg:pl-[17rem] = sidebar w-60 (15rem) + 2rem gap */}
                <main className="flex-1 pt-20 px-4 sm:px-6 md:px-8 pb-20 md:pb-6 md:pl-[17rem] overflow-auto bg-slate-100 dark:bg-zinc-950 min-h-screen">
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
