import {createContext, useContext} from 'react'

interface AdminModeContextType {
    isAdminMode: boolean
    toggleAdminMode: () => void
}

export const AdminModeContext = createContext<AdminModeContextType | undefined>(undefined)

export const useAdminMode = () => {
    const context = useContext(AdminModeContext)
    if (context === undefined) {
        throw new Error('useAdminMode must be used within AdminModeProvider')
    }
    return context
}
