import type {ReactNode} from 'react'

interface TableBodyProps {
    children: ReactNode
    className?: string
}

/**
 * TableBody - Table body section
 * Replaces MUI TableBody with HTML tbody
 */
export const TableBody = ({children, className = ''}: TableBodyProps) => {
    return (
        <tbody className={`divide-y divide-border bg-dark ${className}`}>
        {children}
        </tbody>
    )
}

TableBody.displayName = 'TableBody'
