import type {ReactNode} from 'react'

interface TableHeadProps {
    children: ReactNode
    className?: string
}

/**
 * TableHead - Table header section
 * Replaces MUI TableHead with HTML thead
 */
export const TableHead = ({children, className = ''}: TableHeadProps) => {
    return (
        <thead className={`bg-gray-50 dark:bg-gray-800 border-b border-gray-300 dark:border-gray-700 ${className}`}>
        {children}
        </thead>
    )
}

TableHead.displayName = 'TableHead'
