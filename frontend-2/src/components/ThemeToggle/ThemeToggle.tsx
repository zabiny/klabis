import {useTheme} from '../../theme/ThemeContext'
import {ComputerDesktopIcon, MoonIcon, SunIcon} from '../Icons'

interface ThemeToggleProps {
    className?: string
}

/**
 * ThemeToggle - Button to toggle between light, dark, and system theme modes
 * Cycles: light -> dark -> system -> light
 */
export const ThemeToggle = ({className = ''}: ThemeToggleProps) => {
    const {theme, toggleTheme} = useTheme()

    // Get the icon to display based on current theme
    const getIcon = () => {
        switch (theme) {
            case 'light':
                return <SunIcon size={20}/>
            case 'dark':
                return <MoonIcon size={20}/>
            case 'system':
                return <ComputerDesktopIcon size={20}/>
            default:
                return <MoonIcon size={20}/>
        }
    }

    // Get ARIA label based on current theme (describes the next action)
    const getAriaLabel = () => {
        switch (theme) {
            case 'light':
                return 'Switch to dark mode'
            case 'dark':
                return 'Switch to system preference'
            case 'system':
                return 'Switch to light mode'
            default:
                return 'Toggle theme'
        }
    }

    // Get title text based on current theme
    const getTitle = () => {
        switch (theme) {
            case 'light':
                return 'Light mode'
            case 'dark':
                return 'Dark mode'
            case 'system':
                return 'System preference'
            default:
                return 'Theme'
        }
    }

    return (
        <button
            onClick={toggleTheme}
            className={`px-3 py-2 text-sm text-text-secondary hover:text-text-primary hover:bg-surface-base rounded-md transition-colors duration-base focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-0 ${className}`}
            aria-label={getAriaLabel()}
            title={getTitle()}
        >
            {getIcon()}
        </button>
    )
}

ThemeToggle.displayName = 'ThemeToggle'
