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
        <tbody className={`divide-y divide-gray-200 dark:divide-gray-700 bg-white dark:bg-gray-900 ${className}`}>
        {children}
        </tbody>
    )
}

TableBody.displayName = 'TableBody'
