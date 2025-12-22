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

    const cursorClass = sortable ? 'cursor-pointer hover:bg-surface-raised' : ''
    const fontWeightClass = isSorted ? 'font-semibold text-primary' : 'font-semibold text-text-primary'

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
            className={`px-4 py-3 bg-surface-base text-sm ${fontWeightClass} ${alignClass} ${cursorClass} transition-colors duration-fast uppercase tracking-wider ${className}`}
        >
            <div className="flex items-center gap-2">
                {children}
                {sortable && (
                    <span className="inline-block" aria-hidden="true">
            {isSorted && (
                <span className="text-primary font-semibold">
                {sortDirection === 'asc' ? '↑' : '↓'}
              </span>
            )}
                        {!isSorted && <span className="text-text-tertiary">⋮</span>}
          </span>
                )}
            </div>
        </th>
    )
}

TableHeaderCell.displayName = 'TableHeaderCell'
