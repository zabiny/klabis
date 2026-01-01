import {useEffect} from 'react'

export interface ToastMessage {
    id: string
    message: string
    type: 'success' | 'error' | 'warning' | 'info'
    duration?: number
    action?: {
        label: string
        onClick: () => void
    }
}

interface ToastProps {
    toast: ToastMessage
    onClose: (id: string) => void
}

/**
 * Toast - Notification component
 * Individual toast message display
 */
export const Toast = ({toast, onClose}: ToastProps) => {
    useEffect(() => {
        if (!toast.duration) return

        const timer = setTimeout(() => {
            onClose(toast.id)
        }, toast.duration)

        return () => clearTimeout(timer)
    }, [toast.id, toast.duration, onClose])

    const bgClass = {
        success: 'bg-alert-success border-l-4 border-l-feedback-success border-b border-r border-feedback-success',
        error: 'bg-alert-error border-l-4 border-l-feedback-error border-b border-r border-feedback-error',
        warning: 'bg-alert-warning border-l-4 border-l-feedback-warning border-b border-r border-feedback-warning',
        info: 'bg-alert-info border-l-4 border-l-feedback-info border-b border-r border-feedback-info'
    }[toast.type]

    const iconClass = {
        success: 'text-feedback-success',
        error: 'text-feedback-error',
        warning: 'text-feedback-warning',
        info: 'text-feedback-info'
    }[toast.type]

    const textClass = {
        success: 'text-alert-text-success',
        error: 'text-alert-text-error',
        warning: 'text-alert-text-warning',
        info: 'text-alert-text-info'
    }[toast.type]

    const icons = {
        success: (
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                    fillRule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z"
                    clipRule="evenodd"
                />
            </svg>
        ),
        error: (
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                    fillRule="evenodd"
                    d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                    clipRule="evenodd"
                />
            </svg>
        ),
        warning: (
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                    fillRule="evenodd"
                    d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z"
                    clipRule="evenodd"
                />
            </svg>
        ),
        info: (
            <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                <path
                    fillRule="evenodd"
                    d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z"
                    clipRule="evenodd"
                />
            </svg>
        )
    }

    return (
        <div
            className={`flex items-start gap-3 p-4 rounded-md border-l-4 border-b border-r animate-slide-up ${bgClass}`}>
            <div className={`flex-shrink-0 ${iconClass}`}>{icons[toast.type]}</div>
            <div className="flex-1">
                <p className={`text-sm font-medium ${textClass}`}>{toast.message}</p>
            </div>
            {toast.action && (
                <button
                    onClick={toast.action.onClick}
                    className={`text-sm font-medium ${textClass} hover:opacity-75 transition-opacity`}
                >
                    {toast.action.label}
                </button>
            )}
            <button
                onClick={() => onClose(toast.id)}
                className={`flex-shrink-0 ${textClass} hover:opacity-75 transition-opacity`}
                aria-label="Close"
            >
                <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
                    <path
                        fillRule="evenodd"
                        d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z"
                        clipRule="evenodd"
                    />
                </svg>
            </button>
        </div>
    )
}

/**
 * ToastContainer - Container for displaying multiple toasts
 * Usually rendered at the top-right or bottom-right of the page
 */
interface ToastContainerProps {
    toasts: ToastMessage[]
    onClose: (id: string) => void
    position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left'
}

export const ToastContainer = ({
                                   toasts,
                                   onClose,
                                   position = 'top-right'
                               }: ToastContainerProps) => {
    const positionClass = {
        'top-right': 'top-4 right-4',
        'top-left': 'top-4 left-4',
        'bottom-right': 'bottom-4 right-4',
        'bottom-left': 'bottom-4 left-4'
    }[position]

    return (
        <div
            className={`fixed ${positionClass} z-50 flex flex-col gap-2 max-w-sm pointer-events-none`}
            role="region"
            aria-label="Notifications"
        >
            {toasts.map((toast) => (
                <div key={toast.id} className="pointer-events-auto">
                    <Toast toast={toast} onClose={onClose}/>
                </div>
            ))}
        </div>
    )
}

ToastContainer.displayName = 'ToastContainer'
