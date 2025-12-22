import type {ReactNode} from 'react'

interface TableRowProps {
    children: ReactNode
    onClick?: () => void
    onKeyDown?: (e: React.KeyboardEvent) => void
    hover?: boolean
    tabIndex?: number
    role?: string
    'aria-label'?: string
    className?: string
}

/**
 * TableRow - Table row wrapper
 * Replaces MUI TableRow with HTML tr and Tailwind styling
 */
export const TableRow = ({
                             children,
                             onClick,
                             onKeyDown,
                             hover = false,
                             tabIndex = -1,
                             role,
                             'aria-label': ariaLabel,
                             className = ''
                         }: TableRowProps) => {
    const hoverClass = hover ? 'hover:bg-surface-base border-l-4 border-l-primary' : ''
    const cursorClass = onClick ? 'cursor-pointer' : 'cursor-default'
    const focusClass = tabIndex >= 0 ? 'focus:outline-none focus:ring-2 focus:ring-accent' : ''
    const transitionClass = 'transition-all duration-fast'

    return (
        <tr
            onClick={onClick}
            onKeyDown={onKeyDown}
            tabIndex={tabIndex}
            role={role}
            aria-label={ariaLabel}
            className={`border-l-4 border-l-transparent ${hoverClass} ${cursorClass} ${focusClass} ${transitionClass} ${className}`}
        >
            {children}
        </tr>
    )
}

TableRow.displayName = 'TableRow'
