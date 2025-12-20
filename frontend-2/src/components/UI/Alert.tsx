import type {ReactNode} from 'react'
import clsx from 'clsx'

interface AlertProps {
    severity?: 'success' | 'error' | 'warning' | 'info'
    children: ReactNode
    className?: string
    onClose?: () => void
}

const severityClasses = {
    success: 'bg-green-50 dark:bg-green-900/30 border-green-200 dark:border-green-700 text-green-800 dark:text-green-200',
    error: 'bg-red-50 dark:bg-red-900/30 border-red-200 dark:border-red-700 text-red-800 dark:text-red-200',
    warning: 'bg-yellow-50 dark:bg-yellow-900/30 border-yellow-200 dark:border-yellow-700 text-yellow-800 dark:text-yellow-200',
    info: 'bg-blue-50 dark:bg-blue-900/30 border-blue-200 dark:border-blue-700 text-blue-800 dark:text-blue-200',
}

/**
 * Alert component - Replaces MUI Alert
 * Displays messages with different severity levels
 */
export const Alert = ({
                          severity = 'info',
                          children,
                          className,
                          onClose,
                      }: AlertProps) => {
    const classes = clsx(
        'w-full px-4 py-3 rounded-lg border flex items-start justify-between gap-4',
        severityClasses[severity],
        className
    )

    return (
        <div className={classes} role="alert">
            <div className="flex-1">{children}</div>
            {onClose && (
                <button
                    onClick={onClose}
                    className="flex-shrink-0 text-inherit hover:opacity-70 transition-opacity"
                    aria-label="Close alert"
                >
                    <span className="text-xl">×</span>
                </button>
            )}
        </div>
    )
}

Alert.displayName = 'Alert'
