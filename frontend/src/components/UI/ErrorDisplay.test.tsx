import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {ErrorDisplay} from './ErrorDisplay'
import {vi} from 'vitest'
import {FetchError} from '../../api/authorizedFetch'
import type {FormValidationError} from '../../api/hateoas'

describe('ErrorDisplay Component', () => {
    const mockFetchError = new FetchError(
        'Failed to fetch data',
        500,
        'Internal Server Error',
        new Headers()
    )

    const mockValidationError: FormValidationError = {
        name: 'FormValidationError',
        message: 'Validation failed',
        validationErrors: {
            name: 'Name is required',
            email: 'Invalid email format',
        },
        formData: {},
    } as FormValidationError

    describe('Rendering', () => {
        it('should not render when error is null', () => {
            const {container} = render(<ErrorDisplay error={null}/>)
            expect(container.firstChild).toBeNull()
        })

        it('should not render when error is undefined', () => {
            const {container} = render(<ErrorDisplay error={undefined}/>)
            expect(container.firstChild).toBeNull()
        })

        it('should render error alert with message', () => {
            render(<ErrorDisplay error={mockFetchError}/>)
            expect(screen.getByText('Failed to fetch data')).toBeInTheDocument()
        })

        it('should render error title when provided', () => {
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    title="Load Error"
                />
            )
            expect(screen.getByText('Load Error')).toBeInTheDocument()
            expect(screen.getByText('Failed to fetch data')).toBeInTheDocument()
        })

        it('should use custom message instead of error message', () => {
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    customMessage="Custom error message"
                />
            )
            expect(screen.getByText('Custom error message')).toBeInTheDocument()
            expect(screen.queryByText('Failed to fetch data')).not.toBeInTheDocument()
        })

        it('should render subtitle when provided', () => {
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    subtitle="Endpoint: /api/test"
                />
            )
            expect(screen.getByText('Endpoint: /api/test')).toBeInTheDocument()
        })

        it('should apply custom className', () => {
            const {container} = render(
                <ErrorDisplay
                    error={mockFetchError}
                    className="custom-error-class"
                />
            )
            const alertElement = container.querySelector('[role="alert"]')
            expect(alertElement).toHaveClass('custom-error-class')
        })
    })

    describe('Validation Errors', () => {
        it('should display validation field errors', () => {
            const {container} = render(<ErrorDisplay error={mockValidationError}/>)
            expect(container.textContent).toContain('Name is required')
            expect(container.textContent).toContain('Invalid email format')
        })

        it('should not display validation errors for non-validation errors', () => {
            const {container} = render(<ErrorDisplay error={mockFetchError}/>)
            expect(container.textContent).not.toContain('Name is required')
        })

        it('should display message before validation errors', () => {
            const {container} = render(<ErrorDisplay error={mockValidationError}/>)
            const message = screen.getByText('Validation failed')
            const ul = container.querySelector('ul')
            if (ul) {
                expect(message.compareDocumentPosition(ul) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
            }
        })
    })

    describe('Action Buttons', () => {
        it('should not render buttons when no callbacks provided', () => {
            render(<ErrorDisplay error={mockFetchError}/>)
            expect(screen.queryByTestId('error-display-retry-button')).not.toBeInTheDocument()
            expect(screen.queryByTestId('error-display-cancel-button')).not.toBeInTheDocument()
        })

        it('should render retry button when onRetry provided', () => {
            const mockOnRetry = vi.fn()
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    onRetry={mockOnRetry}
                />
            )
            expect(screen.getByTestId('error-display-retry-button')).toBeInTheDocument()
            expect(screen.getByText('Retry')).toBeInTheDocument()
        })

        it('should render cancel button when onCancel provided', () => {
            const mockOnCancel = vi.fn()
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    onCancel={mockOnCancel}
                />
            )
            expect(screen.getByTestId('error-display-cancel-button')).toBeInTheDocument()
            expect(screen.getByText('Cancel')).toBeInTheDocument()
        })

        it('should render both buttons when both callbacks provided', () => {
            const mockOnRetry = vi.fn()
            const mockOnCancel = vi.fn()
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    onRetry={mockOnRetry}
                    onCancel={mockOnCancel}
                />
            )
            expect(screen.getByTestId('error-display-retry-button')).toBeInTheDocument()
            expect(screen.getByTestId('error-display-cancel-button')).toBeInTheDocument()
        })

        it('should call onRetry when retry button clicked', async () => {
            const user = userEvent.setup()
            const mockOnRetry = vi.fn()
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    onRetry={mockOnRetry}
                />
            )

            const retryButton = screen.getByTestId('error-display-retry-button')
            await user.click(retryButton)

            expect(mockOnRetry).toHaveBeenCalledTimes(1)
        })

        it('should call onCancel when cancel button clicked', async () => {
            const user = userEvent.setup()
            const mockOnCancel = vi.fn()
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    onCancel={mockOnCancel}
                />
            )

            const cancelButton = screen.getByTestId('error-display-cancel-button')
            await user.click(cancelButton)

            expect(mockOnCancel).toHaveBeenCalledTimes(1)
        })

        it('should use custom button texts', () => {
            const mockOnRetry = vi.fn()
            const mockOnCancel = vi.fn()
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    onRetry={mockOnRetry}
                    onCancel={mockOnCancel}
                    retryText="Try Again"
                    cancelText="Dismiss"
                />
            )
            expect(screen.getByText('Try Again')).toBeInTheDocument()
            expect(screen.getByText('Dismiss')).toBeInTheDocument()
        })
    })

    describe('Complex Scenarios', () => {
        it('should display full error with title, subtitle, buttons and validation errors', () => {
            const mockOnRetry = vi.fn()
            const mockOnCancel = vi.fn()
            const {container} = render(
                <ErrorDisplay
                    error={mockValidationError}
                    title="Form Submission Error"
                    subtitle="Check your input fields"
                    onRetry={mockOnRetry}
                    onCancel={mockOnCancel}
                />
            )
            expect(screen.getByText('Form Submission Error')).toBeInTheDocument()
            expect(screen.getByText('Validation failed')).toBeInTheDocument()
            expect(screen.getByText('Check your input fields')).toBeInTheDocument()
            expect(container.textContent).toContain('Name is required')
            expect(screen.getByTestId('error-display-retry-button')).toBeInTheDocument()
            expect(screen.getByTestId('error-display-cancel-button')).toBeInTheDocument()
        })

        it('should prioritize custom message over error message', () => {
            const {container} = render(
                <ErrorDisplay
                    error={mockValidationError}
                    title="Form Error"
                    customMessage="Please fix the validation errors below"
                />
            )
            expect(screen.getByText('Please fix the validation errors below')).toBeInTheDocument()
            expect(screen.queryByText('Validation failed')).not.toBeInTheDocument()
            expect(container.textContent).toContain('Name is required')
        })
    })

    describe('Accessibility', () => {
        it('should have alert role for semantic meaning', () => {
            const {container} = render(<ErrorDisplay error={mockFetchError}/>)
            const alertElement = container.querySelector('[role="alert"]')
            expect(alertElement).toBeInTheDocument()
        })

        it('should have proper button text for screen readers', () => {
            const mockOnRetry = vi.fn()
            const mockOnCancel = vi.fn()
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    onRetry={mockOnRetry}
                    onCancel={mockOnCancel}
                    retryText="Retry Loading"
                    cancelText="Cancel Operation"
                />
            )
            expect(screen.getByRole('button', {name: 'Retry Loading'})).toBeInTheDocument()
            expect(screen.getByRole('button', {name: 'Cancel Operation'})).toBeInTheDocument()
        })
    })
})
