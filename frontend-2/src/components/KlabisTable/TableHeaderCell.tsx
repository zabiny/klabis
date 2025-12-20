import type {ReactNode} from 'react'

interface TableHeaderCellProps {
    children: ReactNode
    align?: 'left' | 'center' | 'right'
    sortable?: boolean
    sortDirection?: 'asc' | 'desc'
    isSorted?: boolean
    onSort?: () => void
    className?: string
}

/**
 * TableHeaderCell - Table header cell (th)
 * Replaces MUI TableCell for header with HTML th and custom sort label
 */
export const TableHeaderCell = ({
                                    children,
                                    align = 'left',
                                    sortable = false,
                                    sortDirection,
                                    isSorted = false,
                                    onSort,
                                    className = ''
                                }: TableHeaderCellProps) => {
    const alignClass = {
        left: 'text-left',
        center: 'text-center',
        right: 'text-right'
    }[align]

    const cursorClass = sortable ? 'cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700' : ''
    const fontWeightClass = isSorted ? 'font-semibold' : 'font-medium'

    const handleClick = () => {
        if (sortable && onSort) {
            onSort()
        }
    }

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (sortable && (e.key === 'Enter' || e.key === ' ')) {
            e.preventDefault()
            handleClick()
        }
    }

    return (
        <th
            onClick={handleClick}
            onKeyDown={handleKeyDown}
            tabIndex={sortable ? 0 : -1}
            role={sortable ? 'button' : undefined}
            aria-sort={
                !sortable ? undefined : isSorted ? (sortDirection === 'asc' ? 'ascending' : 'descending') : 'none'
            }
            className={`px-6 py-3 bg-gray-50 dark:bg-gray-800 text-sm ${fontWeightClass} text-gray-700 dark:text-gray-200 ${alignClass} ${cursorClass} transition-colors ${className}`}
        >
            <div className="flex items-center gap-2">
                {children}
                {sortable && (
                    <span className="inline-block" aria-hidden="true">
            {isSorted && (
                <span className="text-primary">
                {sortDirection === 'asc' ? '↑' : '↓'}
              </span>
            )}
                        {!isSorted && <span className="text-gray-400 dark:text-gray-500">⋮</span>}
          </span>
                )}
            </div>
        </th>
    )
}

TableHeaderCell.displayName = 'TableHeaderCell'
