import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {describe, expect, it, vi} from 'vitest'
import {ModalOverlay} from './ModalOverlay'

describe('ModalOverlay', () => {
    describe('Rendering', () => {
        it('should not render when isOpen is false', () => {
            const {container} = render(
                <ModalOverlay isOpen={false} onClose={() => {
                }}>
                    <div>Modal Content</div>
                </ModalOverlay>
            )
            expect(container.firstChild).toBeNull()
        })

        it('should render when isOpen is true', () => {
            render(
                <ModalOverlay isOpen={true} onClose={() => {
                }}>
                    <div>Modal Content</div>
                </ModalOverlay>
            )
            expect(screen.getByText('Modal Content')).toBeInTheDocument()
        })

        it('should render children content', () => {
            render(
                <ModalOverlay isOpen={true} onClose={() => {
                }}>
                    <div data-testid="test-content">Test Content</div>
                </ModalOverlay>
            )
            expect(screen.getByTestId('test-content')).toBeInTheDocument()
        })
    })

    describe('Backdrop Click Behavior', () => {
        it('should call onClose when backdrop is clicked by default', async () => {
            const user = userEvent.setup()
            const onClose = vi.fn()

            render(
                <ModalOverlay isOpen={true} onClose={onClose}>
                    <div>Modal Content</div>
                </ModalOverlay>
            )

            const backdrop = screen.getByRole('dialog')
            await user.click(backdrop)

            expect(onClose).toHaveBeenCalledTimes(1)
        })

        it('should not call onClose when content is clicked', async () => {
            const user = userEvent.setup()
            const onClose = vi.fn()

            render(
                <ModalOverlay isOpen={true} onClose={onClose}>
                    <div data-testid="content">Modal Content</div>
                </ModalOverlay>
            )

            const content = screen.getByTestId('content')
            await user.click(content)

            expect(onClose).not.toHaveBeenCalled()
        })

        it('should not call onClose when backdrop is clicked and closeOnBackdropClick is false', async () => {
            const user = userEvent.setup()
            const onClose = vi.fn()

            render(
                <ModalOverlay isOpen={true} onClose={onClose} closeOnBackdropClick={false}>
                    <div>Modal Content</div>
                </ModalOverlay>
            )

            const backdrop = screen.getByRole('dialog')
            await user.click(backdrop)

            expect(onClose).not.toHaveBeenCalled()
        })
    })

    describe('Max Width Prop', () => {
        it('should apply default max-w-2xl class', () => {
            const {container} = render(
                <ModalOverlay isOpen={true} onClose={() => {
                }}>
                    <div>Content</div>
                </ModalOverlay>
            )
            const contentDiv = container.querySelector('.max-w-2xl')
            expect(contentDiv).toBeInTheDocument()
        })

        it('should apply max-w-md when maxWidth is md', () => {
            const {container} = render(
                <ModalOverlay isOpen={true} onClose={() => {
                }} maxWidth="md">
                    <div>Content</div>
                </ModalOverlay>
            )
            const contentDiv = container.querySelector('.max-w-md')
            expect(contentDiv).toBeInTheDocument()
        })

        it('should apply max-w-4xl when maxWidth is 4xl', () => {
            const {container} = render(
                <ModalOverlay isOpen={true} onClose={() => {
                }} maxWidth="4xl">
                    <div>Content</div>
                </ModalOverlay>
            )
            const contentDiv = container.querySelector('.max-w-4xl')
            expect(contentDiv).toBeInTheDocument()
        })
    })

    describe('Accessibility', () => {
        it('should have dialog role', () => {
            render(
                <ModalOverlay isOpen={true} onClose={() => {
                }}>
                    <div>Content</div>
                </ModalOverlay>
            )
            expect(screen.getByRole('dialog')).toBeInTheDocument()
        })

        it('should have aria-modal attribute', () => {
            render(
                <ModalOverlay isOpen={true} onClose={() => {
                }}>
                    <div>Content</div>
                </ModalOverlay>
            )
            const dialog = screen.getByRole('dialog')
            expect(dialog).toHaveAttribute('aria-modal', 'true')
        })
    })

    describe('Styling', () => {
        it('should have fixed positioning and z-index', () => {
            const {container} = render(
                <ModalOverlay isOpen={true} onClose={() => {
                }}>
                    <div>Content</div>
                </ModalOverlay>
            )
            const backdrop = container.querySelector('.fixed.inset-0.z-50')
            expect(backdrop).toBeInTheDocument()
        })

        it('should have scrollable content with max-height', () => {
            const {container} = render(
                <ModalOverlay isOpen={true} onClose={() => {
                }}>
                    <div>Content</div>
                </ModalOverlay>
            )
            const content = container.querySelector('.max-h-\\[90vh\\].overflow-y-auto')
            expect(content).toBeInTheDocument()
        })
    })
})
