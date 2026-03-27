import clsx from 'clsx'
import type {ReactNode} from 'react'

interface BadgeProps {
    children: ReactNode
    variant?: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info' | 'orange' | 'blue'
    size?: 'sm' | 'md' | 'lg'
    className?: string
}

const variantClasses = {
    default: 'bg-surface-raised text-text-primary border border-border',
    primary: 'bg-primary/20 text-primary-light',
    success: 'bg-success/20 text-success',
    warning: 'bg-warning/20 text-warning',
    error: 'bg-error/20 text-error',
    info: 'bg-info/20 text-info',
    orange: 'bg-orange-50 text-orange-700',
    blue: 'bg-blue-50 text-blue-700',
}

const sizeClasses = {
    sm: 'px-2 py-1 text-xs font-medium rounded',
    md: 'px-3 py-1.5 text-sm font-medium rounded-md',
    lg: 'px-4 py-2 text-base font-medium rounded-lg',
}

export const Badge = ({
                          children,
                          variant = 'default',
                          size = 'md',
                          className,
                      }: BadgeProps) => {
    return (
        <span className={clsx('inline-block', variantClasses[variant], sizeClasses[size], className)}>
      {children}
    </span>
    )
}

Badge.displayName = 'Badge'
