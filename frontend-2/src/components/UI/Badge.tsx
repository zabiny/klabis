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
        default: 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-100',
        primary: 'bg-primary-100 dark:bg-primary-900 text-primary-900 dark:text-primary-100',
        success: 'bg-green-100 dark:bg-green-900 text-green-900 dark:text-green-100',
        warning: 'bg-yellow-100 dark:bg-yellow-900 text-yellow-900 dark:text-yellow-100',
        error: 'bg-red-100 dark:bg-red-900 text-red-900 dark:text-red-100',
        info: 'bg-blue-100 dark:bg-blue-900 text-blue-900 dark:text-blue-100'
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
