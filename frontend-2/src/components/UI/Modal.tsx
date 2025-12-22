import type {ReactNode} from 'react'

interface ModalProps {
    isOpen: boolean
    onClose: () => void
    title?: string
    children: ReactNode
    footer?: ReactNode
    closeButton?: boolean
    size?: 'sm' | 'md' | 'lg' | 'xl'
    className?: string
}

/**
 * Modal - Dialog/Modal overlay component
 * Replaces MUI Dialog with Tailwind styling
 */
export const Modal = ({
                          isOpen,
                          onClose,
                          title,
                          children,
                          footer,
                          closeButton = true,
                          size = 'md',
                          className = ''
                      }: ModalProps) => {
    const sizeClass = {
        sm: 'max-w-sm',
        md: 'max-w-md',
        lg: 'max-w-lg',
        xl: 'max-w-xl'
    }[size]

    if (!isOpen) return null

    return (
        <>
            {/* Backdrop */}
            <div
                className="fixed inset-0 z-40 bg-black bg-opacity-60 transition-opacity duration-base"
                onClick={onClose}
                aria-hidden="true"
            />

            {/* Modal Container */}
            <div
                className="fixed inset-0 z-50 flex items-center justify-center p-4"
                role="dialog"
                aria-modal="true"
                aria-labelledby={title ? 'modal-title' : undefined}
            >
                {/* Modal Content */}
                <div
                    className={`bg-surface-raised rounded-md shadow-lg w-full ${sizeClass} animate-scale-in ${className}`}>
                    {/* Header */}
                    {(title || closeButton) && (
                        <div
                            className="flex items-center justify-between border-b border-border px-6 py-4 bg-surface-base rounded-t-md">
                            {title && (
                                <h2 id="modal-title" className="text-lg font-semibold text-text-primary font-display">
                                    {title}
                                </h2>
                            )}
                            {closeButton && (
                                <button
                                    onClick={onClose}
                                    className="ml-auto text-text-secondary hover:text-text-primary transition-colors duration-base"
                                    aria-label="Close modal"
                                >
                                    <svg
                                        className="w-6 h-6"
                                        fill="none"
                                        stroke="currentColor"
                                        viewBox="0 0 24 24"
                                    >
                                        <path
                                            strokeLinecap="round"
                                            strokeLinejoin="round"
                                            strokeWidth={2}
                                            d="M6 18L18 6M6 6l12 12"
                                        />
                                    </svg>
                                </button>
                            )}
                        </div>
                    )}

                    {/* Body */}
                    <div className="px-6 py-4 text-text-primary">{children}</div>

                    {/* Footer */}
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
