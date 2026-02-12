import {useTheme} from '../../theme/ThemeContext'

interface ThemeToggleProps {
    className?: string
    variant?: 'icon' | 'button'
}

/**
 * ThemeToggle - Button to toggle between light and dark theme
 * Cycles: light <-> dark
 */
export const ThemeToggle = ({className = '', variant = 'icon'}: ThemeToggleProps) => {
    const {theme, toggleTheme} = useTheme()

    // Get icon based on current theme
    const getIcon = () => {
        if (theme === 'light') {
            return (
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
                    <circle cx="12" cy="12" r="5" />
                    <path strokeLinecap="round" d="M12 1v2m0 4v2m0 4v2" />
                    <path strokeLinecap="round" d="M4.93 4.93l1.41 1.41m13.36 0l1.41-1.41M5 19l4-4m11 4l-4-4" />
                </svg>
            )
        }
        return (
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24" strokeWidth={2}>
                <path strokeLinecap="round" d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
                <path strokeLinecap="round" d="M5 19l4-4m11 4l-4-4" />
            </svg>
        )
    }

    // Get ARIA label (describes next action)
    const getAriaLabel = () => {
        return theme === 'light'
            ? 'Přepnout do tmavého režimu'
            : 'Přepnout do světlého režimu'
    }

    // Get title text (current state)
    const getTitle = () => {
        return theme === 'light'
            ? 'Světlý režim (klikně pro tmavý)'
            : 'Tmavý režim (klikně pro světlý)'
    }

    const baseClasses = variant === 'icon'
        ? 'p-2.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-bg-subtle transition-all duration-fast focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2'
        : 'px-4 py-2 rounded-lg text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-bg-subtle border border-border-subtle hover:border-border-default transition-all duration-base focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2'

    return (
        <button
            onClick={toggleTheme}
            className={`${baseClasses} ${className}`}
            aria-label={getAriaLabel()}
            title={getTitle()}
            type="button"
        >
            {getIcon()}
        </button>
    )
}

ThemeToggle.displayName = 'ThemeToggle'
