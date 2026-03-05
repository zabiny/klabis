import type {ReactNode} from 'react'

interface BadgeProps {
    children: ReactNode
    variant?: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'
    size?: 'sm' | 'md' | 'lg'
    className?: string
}

/**
 * Badge - Status indicator component
 * Replaces MUI Chip with Tailwind styling
 */
export const Badge = ({
                          children,
                          variant = 'default',
                          size = 'md',
                          className = ''
                      }: BadgeProps) => {
    const variantClass = {
        default: 'bg-surface-raised text-text-primary border border-border',
        primary: 'bg-primary/20 text-primary-light',
        success: 'bg-feedback-success/20 text-feedback-success',
        warning: 'bg-feedback-warning/20 text-feedback-warning',
        error: 'bg-feedback-error/20 text-feedback-error',
        info: 'bg-feedback-info/20 text-feedback-info'
    }[variant]

    const sizeClass = {
        sm: 'px-2 py-1 text-xs font-medium rounded',
        md: 'px-3 py-1.5 text-sm font-medium rounded-md',
        lg: 'px-4 py-2 text-base font-medium rounded-lg'
    }[size]

    return (
        <span className={`inline-block ${variantClass} ${sizeClass} ${className}`}>
      {children}
    </span>
    )
}

Badge.displayName = 'Badge'
