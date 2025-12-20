import type {ReactNode} from 'react'

interface CardProps {
    children: ReactNode
    className?: string
    hoverable?: boolean
    shadow?: 'sm' | 'md' | 'lg' | 'none'
}

/**
 * Card - Container component for grouping related content
 * Replaces MUI Paper with Tailwind styling
 */
export const Card = ({
                         children,
                         className = '',
                         hoverable = false,
                         shadow = 'md'
                     }: CardProps) => {
    const shadowClass = {
        none: '',
        sm: 'shadow-sm',
        md: 'shadow-md',
        lg: 'shadow-lg'
    }[shadow]

    const hoverClass = hoverable ? 'hover:shadow-lg transition-shadow cursor-pointer' : ''

    return (
        <div
            className={`bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700 ${shadowClass} ${hoverClass} ${className}`}
        >
            {children}
        </div>
    )
}

Card.displayName = 'Card'
