import type {ReactNode} from 'react'
import {createContext, useContext, useEffect, useState} from 'react'

type Theme = 'light' | 'dark'

interface ThemeContextType {
    theme: Theme
    toggleTheme: () => void
    setTheme: (theme: Theme) => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

/**
 * ThemeProvider component for managing dark/light mode
 * Wraps the application to provide theme context
 */
export const ThemeProvider = ({children}: { children: ReactNode }) => {
    const [theme, setThemeState] = useState<Theme>('dark')
    const [mounted, setMounted] = useState(false)

    // Initialize theme from localStorage or system preference
    useEffect(() => {
        const savedTheme = localStorage.getItem('theme') as Theme | null
        const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches

        const initialTheme = savedTheme || (prefersDark ? 'dark' : 'light')
        setThemeState(initialTheme)
        applyTheme(initialTheme)
        setMounted(true)
    }, [])

    const applyTheme = (newTheme: Theme) => {
        const htmlElement = document.documentElement
        if (newTheme === 'dark') {
            htmlElement.classList.add('dark')
        } else {
            htmlElement.classList.remove('dark')
        }
        localStorage.setItem('theme', newTheme)
    }

    const setTheme = (newTheme: Theme) => {
        setThemeState(newTheme)
        applyTheme(newTheme)
    }

    const toggleTheme = () => {
        const newTheme = theme === 'dark' ? 'light' : 'dark'
        setTheme(newTheme)
    }

    // Prevent flash of unstyled content
    if (!mounted) {
        return <>{children}</>
    }

    return (
        <ThemeContext.Provider value={{theme, toggleTheme, setTheme}}>
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
