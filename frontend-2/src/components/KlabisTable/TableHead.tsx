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
        <thead className={`bg-surface-base border-b-2 border-border ${className}`}>
        {children}
        </thead>
    )
}

TableHead.displayName = 'TableHead'
