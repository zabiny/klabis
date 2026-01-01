import type {ReactNode} from 'react'
import {modalStyles} from '../../theme/designTokens'

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
}

const maxWidthClasses = {
    md: 'max-w-md',
    lg: 'max-w-lg',
    xl: 'max-w-xl',
    '2xl': 'max-w-2xl',
    '4xl': 'max-w-4xl'
}

/**
 * Simple modal overlay wrapper for displaying content in a centered modal
 * Designed for cases where you need a simple overlay without structured header/footer
 * For structured modals with header/footer, use the Modal component instead
 */
export function ModalOverlay({
                                 isOpen,
                                 onClose,
                                 children,
                                 maxWidth = '2xl',
                                 closeOnBackdropClick = true
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
        >
            <div
                className={`${modalStyles.content} ${maxWidthClasses[maxWidth]}`}
                onClick={handleContentClick}
            >
                {children}
            </div>
        </div>
    )
}

ModalOverlay.displayName = 'ModalOverlay'
