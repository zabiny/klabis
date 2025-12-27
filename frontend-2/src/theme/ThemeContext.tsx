import type {ReactNode} from 'react'
import {createContext, useContext, useEffect, useState} from 'react'

type Theme = 'light' | 'dark' | 'system'
type EffectiveTheme = 'light' | 'dark'

interface ThemeContextType {
    theme: Theme
    effectiveTheme: EffectiveTheme
    toggleTheme: () => void
    setTheme: (theme: Theme) => void
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined)

/**
 * Helper function to resolve system theme preference
 */
const getSystemThemePreference = (): EffectiveTheme => {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

/**
 * Helper function to get the effective theme (resolves 'system' to actual theme)
 */
const getEffectiveTheme = (theme: Theme): EffectiveTheme => {
    if (theme === 'system') {
        return getSystemThemePreference()
    }
    return theme as EffectiveTheme
}

/**
 * ThemeProvider component for managing dark/light/system mode
 * Wraps the application to provide theme context
 */
export const ThemeProvider = ({children}: { children: ReactNode }) => {
    const [theme, setThemeState] = useState<Theme>('dark')
    const [effectiveTheme, setEffectiveTheme] = useState<EffectiveTheme>('dark')

    // Initialize theme from localStorage or system preference
    useEffect(() => {
        try {
            const savedTheme = localStorage.getItem('theme') as Theme | null
            const initialTheme = (savedTheme || 'dark') as Theme
            setThemeState(initialTheme)
            const effective = getEffectiveTheme(initialTheme)
            setEffectiveTheme(effective)
            applyTheme(initialTheme, effective)
        } catch {
            // Handle cases where localStorage is not available (e.g., private browsing)
            const systemPref = getSystemThemePreference()
            setThemeState('system')
            setEffectiveTheme(systemPref)
            applyTheme('system', systemPref)
        }
    }, [])

    // Listen for system preference changes when theme is 'system'
    useEffect(() => {
        if (theme !== 'system') return

        const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
        const handleChange = () => {
            const newEffectiveTheme = getSystemThemePreference()
            setEffectiveTheme(newEffectiveTheme)
            applyTheme('system', newEffectiveTheme)
        }

        mediaQuery.addEventListener('change', handleChange)

        return () => {
            mediaQuery.removeEventListener('change', handleChange)
        }
    }, [theme])

    const applyTheme = (newTheme: Theme, effective: EffectiveTheme) => {
        const htmlElement = document.documentElement
        if (effective === 'dark') {
            htmlElement.classList.add('dark')
        } else {
            htmlElement.classList.remove('dark')
        }
        try {
            localStorage.setItem('theme', newTheme)
        } catch {
            // Handle cases where localStorage is not available (e.g., private browsing)
            // Theme is already applied, just can't persist it
        }
    }

    const setTheme = (newTheme: Theme) => {
        setThemeState(newTheme)
        const effective = getEffectiveTheme(newTheme)
        setEffectiveTheme(effective)
        applyTheme(newTheme, effective)
    }

    const toggleTheme = () => {
        const themes: Theme[] = ['light', 'dark', 'system']
        const currentIndex = themes.indexOf(theme)
        const nextIndex = (currentIndex + 1) % themes.length
        setTheme(themes[nextIndex])
    }

    return (
        <ThemeContext.Provider value={{theme, effectiveTheme, toggleTheme, setTheme}}>
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
