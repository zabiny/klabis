import type {ReactNode} from 'react'
import {createContext, useContext, useEffect, useState} from 'react'

type Theme = 'light' | 'dark'

interface ThemeContextType {
    theme: Theme
    toggleTheme: () => void
    setTheme: (theme: Theme) => void
    isDark: boolean
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

/**
 * Helper function to get initial theme from localStorage or system preference
 */
const getInitialTheme = (): Theme => {
    try {
        const savedTheme = localStorage.getItem('theme') as Theme | null
        if (savedTheme === 'light' || savedTheme === 'dark') {
            return savedTheme
        }
    } catch {
        // localStorage not available
    }
    // Fallback to system preference
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/**
 * Apply theme to HTML element
 */
const applyThemeToDOM = (theme: Theme) => {
    const htmlElement = document.documentElement
    if (theme === 'dark') {
        htmlElement.classList.add('dark')
    } else {
        htmlElement.classList.remove('dark')
    }
}

/**
 * ThemeProvider component for managing dark/light mode
 * Wraps application to provide theme context
 */
export const ThemeProvider = ({children}: { children: ReactNode }) => {
    const [theme, setThemeState] = useState<Theme>(getInitialTheme)

    // Apply theme on mount and when theme changes
    useEffect(() => {
        applyThemeToDOM(theme)

        try {
            localStorage.setItem('theme', theme)
        } catch {
            // localStorage not available (private browsing) - theme is still applied
        }
    }, [theme])

    const setTheme = (newTheme: Theme) => {
        setThemeState(newTheme)
        applyThemeToDOM(newTheme)
    }

    const toggleTheme = () => {
        setThemeState(prev => prev === 'light' ? 'dark' : 'light')
    }

    return (
        <ThemeContext.Provider value={{
            theme,
            toggleTheme,
            setTheme,
            isDark: theme === 'dark'
        }}>
            {children}
        </ThemeContext.Provider>
    )
}

/**
 * Hook to use theme context
 */
export const useTheme = () => {
    const context = useContext(ThemeContext)
    if (context === undefined) {
        throw new Error('useTheme must be used within ThemeProvider')
    }
    return context
}
