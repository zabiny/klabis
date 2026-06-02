import type {ReactNode} from 'react'
import {useEffect, useMemo, useState} from 'react'
import {useIsAdmin} from '../hooks/useIsAdmin'
import {AdminModeContext} from './adminModeContext'

/**
 * AdminModeProvider component for managing admin-only route limiting mode
 *
 * When admin mode is enabled, most routes are hidden and only these pages remain accessible:
 * - HomePage (/, /index.html)
 * - SandplacePage (/sandplace)
 * - GenericHalPage (* catch-all)
 *
 * Important behavior:
 * - Only admin users can use admin mode (non-admins always see isAdminMode: false)
 * - State persists in localStorage under 'adminMode' key
 * - Non-admin users' localStorage values are preserved but ignored
 * - This allows admins to retain their settings across sessions
 */
export const AdminModeProvider = ({children}: { children: ReactNode }) => {
    const {isAdmin} = useIsAdmin()
    const [storedAdminMode, setStoredAdminMode] = useState<boolean>(false)

    // Initialize admin mode from localStorage
    useEffect(() => {
        try {
            const savedMode = localStorage.getItem('adminMode')
            if (savedMode !== null) {
                setStoredAdminMode(savedMode === 'true')
            }
        } catch {
            // Handle cases where localStorage is not available (e.g., private browsing)
            // Default to false
            setStoredAdminMode(false)
        }
    }, [])

    // Computed value: only true if user is admin AND stored mode is true
    const isAdminMode = useMemo(() => {
        return isAdmin && storedAdminMode
    }, [isAdmin, storedAdminMode])

    const toggleAdminMode = () => {
        if (!isAdmin) {
            // Non-admins cannot toggle admin mode
            return
        }

        const newValue = !storedAdminMode
        setStoredAdminMode(newValue)

        try {
            localStorage.setItem('adminMode', String(newValue))
        } catch {
            // Handle cases where localStorage is not available
            // Mode is already toggled in state, just can't persist it
        }
    }

    return (
        <AdminModeContext.Provider value={{isAdminMode, toggleAdminMode}}>
            {children}
        </AdminModeContext.Provider>
    )
}
