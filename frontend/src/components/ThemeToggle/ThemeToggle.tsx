import {Sun, Moon} from 'lucide-react'
import {useTheme} from '../../theme/ThemeContext'
import {labels} from '../../localization'

interface ThemeToggleProps {
    className?: string
    variant?: 'icon' | 'button'
}

export const ThemeToggle = ({className = '', variant = 'icon'}: ThemeToggleProps) => {
    const {theme, toggleTheme} = useTheme()

    const baseClasses = variant === 'icon'
        ? 'p-2.5 rounded-lg text-text-secondary hover:text-text-primary hover:bg-bg-subtle transition-all duration-fast focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2'
        : 'px-4 py-2 rounded-lg text-sm font-medium text-text-secondary hover:text-text-primary hover:bg-bg-subtle border border-border-subtle hover:border-border-default transition-all duration-base focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2'

    return (
        <button
            onClick={toggleTheme}
            className={`${baseClasses} ${className}`}
            aria-label={theme === 'light' ? labels.ui.switchToDark : labels.ui.switchToLight}
            title={theme === 'light' ? labels.ui.switchToDark : labels.ui.switchToLight}
            type="button"
        >
            {theme === 'light' ? <Moon className="w-5 h-5" /> : <Sun className="w-5 h-5" />}
        </button>
    )
}

ThemeToggle.displayName = 'ThemeToggle'
