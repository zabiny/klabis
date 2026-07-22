import type {ButtonHTMLAttributes, ReactNode} from 'react'
import clsx from 'clsx'
import {twMerge} from 'tailwind-merge'

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'warning' | 'ghost' | 'danger-ghost' | 'primary-ghost' | 'warning-ghost'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: ButtonVariant
    size?: 'sm' | 'md' | 'lg'
    fullWidth?: boolean
    loading?: boolean
    children: ReactNode
    startIcon?: ReactNode
    endIcon?: ReactNode
}

const variantClasses: Record<ButtonVariant, string> = {
    primary: 'bg-primary hover:bg-primary-light text-white shadow-sm hover:shadow-md active:shadow-none disabled:opacity-50 disabled:shadow-none',
    secondary: 'bg-surface text-text-primary border border-border hover:border-border-light hover:bg-surface-raised active:bg-surface disabled:opacity-50',
    danger: 'bg-error hover:bg-red-500 text-white shadow-sm hover:shadow-md active:shadow-none disabled:opacity-50 disabled:shadow-none',
    warning: 'bg-warning hover:bg-amber-600 text-white shadow-sm hover:shadow-md active:shadow-none disabled:opacity-50 disabled:shadow-none',
    ghost: 'bg-transparent hover:bg-surface-base text-text-primary active:bg-surface-raised disabled:opacity-50',
    'danger-ghost': 'text-red-600 bg-red-50 hover:bg-red-100 dark:text-red-400 dark:bg-red-950/50 dark:hover:bg-red-950 disabled:opacity-50',
    'primary-ghost': 'text-primary bg-primary-subtle hover:bg-primary/20 dark:text-primary dark:bg-primary/10 dark:hover:bg-primary/20 disabled:opacity-50',
    'warning-ghost': 'text-warning bg-warning-bg hover:bg-warning/20 dark:text-warning dark:bg-warning/10 dark:hover:bg-warning/20 disabled:opacity-50',
}

const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2.5 text-base',
    lg: 'px-6 py-3 text-lg',
}

export const Button = ({
                           variant = 'primary',
                           size = 'md',
                           fullWidth = false,
                           loading = false,
                           disabled = false,
                           children,
                           startIcon,
                           endIcon,
                           className,
                           ...props
                       }: ButtonProps) => {
    const classes = twMerge(clsx(
        'inline-flex items-center justify-center font-medium rounded-md',
        'transition-all duration-fast',
        'focus:outline-none focus:ring-2 focus:ring-accent focus:ring-offset-0',
        'hover:-translate-y-0.5',
        'active:scale-95 active:translate-y-0',
        'disabled:cursor-not-allowed',
        variantClasses[variant],
        sizeClasses[size],
        fullWidth && 'w-full',
        className
    ))

    return (
        <button
            className={classes}
            disabled={disabled || loading}
            {...props}
        >
            {loading && startIcon === undefined && (
                <span
                    className="mr-2 animate-spin inline-block w-4 h-4 border-2 border-current border-t-transparent rounded-full"></span>
            )}
            {!loading && startIcon && <span className="mr-2 flex items-center justify-center">{startIcon}</span>}
            <span>{children}</span>
            {!loading && endIcon && <span className="ml-2 flex items-center justify-center">{endIcon}</span>}
        </button>
    )
}

Button.displayName = 'Button'
