import type {ReactNode} from 'react'

interface TableDataCellProps {
    children: ReactNode
    align?: 'left' | 'center' | 'right'
    colSpan?: number
    className?: string
}

/**
 * TableDataCell - Table data cell (td)
 * Replaces MUI TableCell for body cells with HTML td
 */
export const TableDataCell = ({
                                  children,
                                  align = 'left',
                                  colSpan,
                                  className = ''
                              }: TableDataCellProps) => {
    const alignClass = {
        left: 'text-left',
        center: 'text-center',
        right: 'text-right'
    }[align]

    return (
        <td
            colSpan={colSpan}
            className={`px-6 py-4 text-sm text-gray-900 dark:text-gray-100 ${alignClass} ${className}`}
        >
            {children}
        </td>
    )
}

TableDataCell.displayName = 'TableDataCell'
