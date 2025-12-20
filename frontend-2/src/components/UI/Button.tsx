import type {ButtonHTMLAttributes, ReactNode} from 'react'
import clsx from 'clsx'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
    size?: 'sm' | 'md' | 'lg'
    fullWidth?: boolean
    loading?: boolean
    children: ReactNode
    startIcon?: ReactNode
    endIcon?: ReactNode
}

const variantClasses = {
    primary: 'bg-primary hover:bg-primary-dark text-white disabled:bg-gray-400',
    secondary: 'bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 text-gray-900 dark:text-white disabled:bg-gray-300 dark:disabled:bg-gray-600',
    danger: 'bg-red-500 hover:bg-red-600 text-white disabled:bg-red-300',
    ghost: 'bg-transparent hover:bg-gray-100 dark:hover:bg-gray-800 text-gray-900 dark:text-white disabled:bg-transparent disabled:opacity-50',
}

const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm',
    md: 'px-4 py-2.5 text-base',
    lg: 'px-6 py-3 text-lg',
}

/**
 * Button component - Replaces MUI Button
 * Supports variants, sizes, icons, and loading state
 */
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
    const classes = clsx(
        'inline-flex items-center justify-center font-medium rounded-lg',
        'transition-all duration-200',
        'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0',
        'dark:focus:ring-offset-0',
        'disabled:cursor-not-allowed disabled:opacity-50',
        variantClasses[variant],
        sizeClasses[size],
        fullWidth && 'w-full',
        className
    )

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
