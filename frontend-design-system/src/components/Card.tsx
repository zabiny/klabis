import type {ReactNode} from 'react'
import clsx from 'clsx'

interface CardProps {
    children: ReactNode
    className?: string
    hoverable?: boolean
    shadow?: 'sm' | 'md' | 'lg' | 'none'
}

/**
 * Card - Container component for grouping related content
 * Refined design with subtle border and hover effects
 */
export const Card = ({
                         children,
                         className = '',
                         hoverable = false,
                         shadow = 'sm'
                     }: CardProps) => {
    const shadowClass = {
        none: '',
        sm: 'shadow-sm',
        md: 'shadow-md',
        lg: 'shadow-lg'
    }[shadow]

    const hoverClass = hoverable ? 'hover:shadow-md hover:border-border-light cursor-pointer' : ''
    const transitionClass = hoverable ? 'transition-all duration-base' : ''

    const classes = clsx(
        'bg-surface-raised rounded-md border border-border',
        shadowClass,
        hoverClass,
        transitionClass,
        className
    )

    return (
        <div className={classes}>
            {children}
        </div>
    )
}

Card.displayName = 'Card'
