import type {ReactNode} from 'react'
import {buttonStyles, layoutStyles, modalStyles} from '../../theme/designTokens'
import {UI_MESSAGES} from '../../constants/messages'

interface ModalOverlayProps {
    /** Whether the modal is open */
    isOpen: boolean
    /** Callback when modal should close */
    onClose: () => void
    /** Modal content */
    children: ReactNode
    /** Maximum width of modal (default: 2xl) */
    maxWidth?: 'md' | 'lg' | 'xl' | '2xl' | '4xl'
    /** Whether clicking backdrop closes modal (default: true) */
    closeOnBackdropClick?: boolean
    /** Optional title displayed in modal header */
    title?: string
    /** Whether to show the close button in header when title is provided (default: true) */
    showCloseButton?: boolean
}

const maxWidthClasses = {
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '4xl': 'max-w-4xl'
}

/**
 * Modal overlay wrapper for displaying content in a centered modal.
 * Supports an optional title header with a close button.
 */
export function ModalOverlay({
                                 isOpen,
                                 onClose,
                                 children,
                                 maxWidth = '2xl',
                                 closeOnBackdropClick = true,
                                 title,
                                 showCloseButton = true,
                             }: ModalOverlayProps) {
    if (!isOpen) return null

    const handleBackdropClick = () => {
        if (closeOnBackdropClick) {
            onClose()
        }
    }

    const handleContentClick = (e: React.MouseEvent) => {
        // Prevent clicks inside content from closing modal
        e.stopPropagation()
    }

    return (
        <div
            className={modalStyles.backdrop}
            onClick={handleBackdropClick}
            role="dialog"
            aria-modal="true"
            aria-labelledby={title ? 'modal-overlay-title' : undefined}
        >
            <div
                className={`${modalStyles.content} ${maxWidthClasses[maxWidth]}`}
                onClick={handleContentClick}
            >
                {title && (
                    <div className={layoutStyles.headerRow} data-testid="modal-header">
                        <h4 id="modal-overlay-title" className="font-semibold" data-testid="modal-title">{title}</h4>
                        {showCloseButton && (
                            <button
                                onClick={onClose}
                                className={buttonStyles.closeButton}
                                data-testid="modal-close-button"
                                aria-label="Close modal"
                            >
                                {UI_MESSAGES.CLOSE}
                            </button>
                        )}
                    </div>
                )}
                {children}
            </div>
        </div>
    )
}

ModalOverlay.displayName = 'ModalOverlay'
