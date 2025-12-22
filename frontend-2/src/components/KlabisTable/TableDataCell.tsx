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
            className={`px-4 py-3 text-sm text-text-primary ${alignClass} ${className}`}
        >
            {children}
        </td>
    )
}

TableDataCell.displayName = 'TableDataCell'
