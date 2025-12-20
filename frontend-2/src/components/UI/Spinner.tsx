import clsx from 'clsx'

interface SpinnerProps {
    size?: 'sm' | 'md' | 'lg'
    className?: string
}

const sizeClasses = {
    sm: 'w-4 h-4 border-2',
    md: 'w-6 h-6 border-2',
    lg: 'w-8 h-8 border-3',
}

/**
 * Spinner component - Replaces MUI CircularProgress
 * Loading indicator spinner
 */
export const Spinner = ({
                            size = 'md',
                            className = '',
                        }: SpinnerProps) => {
    const classes = clsx(
        'inline-block rounded-full border-current border-t-transparent',
        'animate-spin',
        sizeClasses[size],
        className
    )

    return (
        <div
            className={classes}
            role="status"
            aria-label="Loading"
        >
            <span className="sr-only">Loading...</span>
        </div>
    )
}

Spinner.displayName = 'Spinner'
