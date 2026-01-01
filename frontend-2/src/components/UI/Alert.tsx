import type {ReactNode} from 'react'
import clsx from 'clsx'

interface AlertProps {
    severity?: 'success' | 'error' | 'warning' | 'info'
    children: ReactNode
    className?: string
    onClose?: () => void
}

const severityClasses = {
    success: 'bg-alert-success border-l-4 border-l-feedback-success border-b border-r border-feedback-success text-alert-text-success',
    error: 'bg-alert-error border-l-4 border-l-feedback-error border-b border-r border-feedback-error text-alert-text-error',
    warning: 'bg-alert-warning border-l-4 border-l-feedback-warning border-b border-r border-feedback-warning text-alert-text-warning',
    info: 'bg-alert-info border-l-4 border-l-feedback-info border-b border-r border-feedback-info text-alert-text-info',
}

/**
 * Alert component - Refined with semantic colors and left border accent
 * Displays messages with different severity levels
 */
export const Alert = ({
                          severity = 'info',
                          children,
                          className,
                          onClose,
                      }: AlertProps) => {
    const classes = clsx(
        'w-full px-4 py-3 rounded-md border-l-4 border-b border-r flex items-start justify-between gap-4 animate-fade-in',
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
