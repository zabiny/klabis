import type {ReactNode} from 'react'

interface TableContainerProps {
    children: ReactNode
    className?: string
}

/**
 * TableContainer - Scrollable container for tables
 * Replaces MUI TableContainer with Tailwind styling
 */
export const TableContainer = ({children, className = ''}: TableContainerProps) => {
    return (
        <div className={`overflow-x-auto ${className}`}>
            {children}
        </div>
    )
}

TableContainer.displayName = 'TableContainer'
