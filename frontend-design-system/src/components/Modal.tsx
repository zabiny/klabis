import type {ReactNode} from 'react'
import {X} from 'lucide-react'

interface ModalProps {
    isOpen: boolean
    onClose: () => void
    title?: string
    children: ReactNode
    footer?: ReactNode
    closeButton?: boolean
    size?: 'sm' | 'md' | 'lg' | 'xl' | '2xl' | '4xl'
    className?: string
    closeOnBackdropClick?: boolean
}

const sizeClasses = {
    sm: 'max-w-sm',
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '4xl': 'max-w-4xl',
}

export const Modal = ({
                          isOpen,
                          onClose,
                          title,
                          children,
                          footer,
                          closeButton = true,
                          size = 'md',
                          className = '',
                          closeOnBackdropClick = true,
                      }: ModalProps) => {

    const handleBackdropClick = () => {
        if (closeOnBackdropClick) {
            onClose()
        }
    }

    const handleContentClick = (e: React.MouseEvent) => {
        e.stopPropagation()
    }

    if (!isOpen) return null

    return (
        <>
            <div
                className="fixed inset-0 z-40 bg-black bg-opacity-60 transition-opacity duration-base"
                onClick={handleBackdropClick}
                aria-hidden="true"
                data-testid="modal-backdrop"
            />

            <div
                className="fixed inset-0 z-50 flex items-center justify-center p-4"
                role="dialog"
                aria-modal="true"
                aria-labelledby={title ? 'modal-title' : undefined}
            >
                <div
                    className={`bg-surface-raised rounded-md shadow-lg w-full ${sizeClasses[size]} animate-scale-in ${className}`}
                    onClick={handleContentClick}
                >
                    {(title || closeButton) && (
                        <div
                            className="flex items-center justify-between border-b border-border px-6 py-4 bg-surface-base rounded-t-md"
                            data-testid="modal-header"
                        >
                            {title && (
                                <h2 id="modal-title" className="text-lg font-semibold text-text-primary font-display" data-testid="modal-title">
                                    {title}
                                </h2>
                            )}
                            {closeButton && (
                                <button
                                    onClick={onClose}
                                    className="ml-auto text-text-secondary hover:text-text-primary transition-colors duration-base"
                                    aria-label="Close modal"
                                    data-testid="modal-close-button"
                                >
                                    <X className="w-6 h-6" />
                                </button>
                            )}
                        </div>
                    )}

                    <div className="px-6 py-4 text-text-primary">{children}</div>

                    {footer && (
                        <div className="border-t border-border px-6 py-4 flex justify-end gap-3">
                            {footer}
                        </div>
                    )}
                </div>
            </div>
        </>
    )
}

Modal.displayName = 'Modal'
