import type {ReactNode} from 'react'

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
            className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 animate-in fade-in duration-300"
            onClick={handleBackdropClick}
            role="dialog"
            aria-modal="true"
        >
            <div
                className={`bg-surface ${maxWidthClasses[maxWidth]} w-full max-h-[90vh] overflow-y-auto p-6 rounded-lg shadow-xl animate-in fade-in zoom-in-95 duration-300`}
                onClick={handleContentClick}
            >
                {children}
            </div>
        </div>
    )
}

ModalOverlay.displayName = 'ModalOverlay'
