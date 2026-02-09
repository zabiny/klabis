import {useAdminMode} from '../../contexts/AdminModeContext'
import {useIsAdmin} from '../../hooks/useIsAdmin'

interface AdminToggleProps {
    className?: string
}

/**
 * AdminToggle - Button to toggle admin-only route limiting mode
 *
 * Only visible to admin users.
 * When enabled, restricts access to only: HomePage, SandplacePage, and GenericHalPage
 */
export const AdminToggle = ({className = ''}: AdminToggleProps) => {
    const {isAdminMode, toggleAdminMode} = useAdminMode()
    const {isAdmin} = useIsAdmin()

    // Don't render for non-admin users
    if (!isAdmin) {
        return null
    }

    // Get the icon to display based on current mode
    const getIcon = () => {
        if (isAdminMode) {
            // Locked shield - indicates active admin mode
            return (
                <svg
                    className="w-5 h-5"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                    />
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M12 9v2m0 4h.01"
                    />
                </svg>
            )
        } else {
            // Open lock/shield - indicates normal mode
            return (
                <svg
                    className="w-5 h-5"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M8 11V7a4 4 0 118 0m-4 8v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2z"
                    />
                </svg>
            )
        }
    }

    // Get ARIA label based on current mode
    const getAriaLabel = () => {
        return isAdminMode
            ? 'Disable admin mode'
            : 'Enable admin mode'
    }

    // Get title text based on current mode
    const getTitle = () => {
        return isAdminMode
            ? 'Admin mód: Zapnuto'
            : 'Admin mód: Vypnuto'
    }

    // Dynamic styling: blue/green when active, normal when inactive
    const activeClass = isAdminMode
        ? 'text-accent hover:text-accent-hover'
        : 'text-text-secondary hover:text-text-primary'

    return (
        <button
            onClick={toggleAdminMode}
            className={`px-3 py-2 text-sm ${activeClass} hover:bg-surface-base rounded-md transition-colors duration-base focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-0 ${className}`}
            aria-label={getAriaLabel()}
            title={getTitle()}
        >
            {getIcon()}
        </button>
    )
}

AdminToggle.displayName = 'AdminToggle'
