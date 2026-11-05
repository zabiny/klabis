import {render, screen} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import {ErrorDisplay} from './ErrorDisplay'
import {vi} from 'vitest'
import {ApiError} from '../../api/authorizedFetch'
import {labels} from '../../localization'

describe('ErrorDisplay Component', () => {
    const mockFetchError = new Error('Failed to fetch data')

    const validationError = new ApiError(400, {
        type: '/errors/validation-failed',
        fieldErrors: {name: 'nesmí být prázdné', email: 'musí být e-mailová adresa'},
    })

    describe('Rendering', () => {
        it('should not render when error is null', () => {
            const {container} = render(<ErrorDisplay error={null}/>)
            expect(container.firstChild).toBeNull()
        })

        it('should not render when error is undefined', () => {
            const {container} = render(<ErrorDisplay error={undefined}/>)
            expect(container.firstChild).toBeNull()
        })

        it('should render the generic Czech text for a plain Error, never its message', () => {
            render(<ErrorDisplay error={mockFetchError}/>)
            expect(screen.getByText(labels.errors.unexpectedError)).toBeInTheDocument()
            expect(screen.queryByText('Failed to fetch data')).not.toBeInTheDocument()
        })

        it('should render error title when provided', () => {
            render(
                <ErrorDisplay
                    error={mockFetchError}
                    title="Load Error"
                />
            )
            expect(screen.getByText('Load Error')).toBeInTheDocument()
            expect(screen.getByText(labels.errors.unexpectedError)).toBeInTheDocument()
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

    describe('ApiError', () => {
        it('shows the Czech text of a typed error instead of the technical message', () => {
            render(<ErrorDisplay error={new ApiError(404, {type: '/errors/not-found', detail: 'English detail'})}/>)

            expect(screen.getByText(labels.errorTypes['/errors/not-found']({}))).toBeInTheDocument()
            expect(screen.queryByText(/HTTP 404/)).not.toBeInTheDocument()
            expect(screen.queryByText(/English detail/)).not.toBeInTheDocument()
        })

        it('falls back to a generic Czech text by status for an unknown type', () => {
            render(<ErrorDisplay error={new ApiError(403, {type: '/errors/new-one'})}/>)

            expect(screen.getByText(labels.errorTypes['/errors/forbidden']({}))).toBeInTheDocument()
        })

        it('shows form-level error texts', () => {
            render(<ErrorDisplay error={new ApiError(400, {type: '/errors/validation-failed', errors: ['Hesla se neshodují']})}/>)

            expect(screen.getByText('Hesla se neshodují')).toBeInTheDocument()
        })

        it('shows the validation heading when only field errors exist', () => {
            render(<ErrorDisplay error={validationError}/>)

            expect(screen.getByText(labels.errors.formValidationErrors)).toBeInTheDocument()
            expect(screen.queryByText(labels.errorTypes['/errors/validation-failed']({}))).not.toBeInTheDocument()
        })

        it('treats a nested key as matched when its first segment is a template property', () => {
            const error = new ApiError(400, {
                type: '/errors/validation-failed',
                fieldErrors: {'address.street': 'nesmí být prázdné'},
            })

            const {container} = render(<ErrorDisplay error={error} fieldPrompts={{address: 'Adresa'}}/>)

            expect(container.textContent).not.toContain('nesmí být prázdné')
            expect(container.textContent).not.toContain('address.street')
        })

        it('shows the generic validation text when validation-failed carries no details', () => {
            const {container} = render(<ErrorDisplay error={new ApiError(400, {type: '/errors/validation-failed'})}/>)

            expect(container.textContent).toContain(labels.errorTypes['/errors/validation-failed']({}))
        })

        it('shows an unmatched field error without its raw key when no property matches', () => {
            const error = new ApiError(400, {type: '/errors/validation-failed', fieldErrors: {unknownKey: 'chyba'}})

            const {container} = render(<ErrorDisplay error={error} fieldPrompts={{name: 'Název'}}/>)

            expect(screen.getByText('chyba')).toBeInTheDocument()
            expect(container.textContent).not.toContain('unknownKey')
        })

        it('omits field errors that are shown at their fields', () => {
            const {container} = render(
                <ErrorDisplay error={validationError} fieldPrompts={{name: 'Název', email: 'E-mail'}}/>
            )

            expect(container.textContent).not.toContain('nesmí být prázdné')
            expect(container.textContent).toContain(labels.errors.formValidationErrors)
        })

        it('renders nothing for a typed error bound to a rendered field', () => {
            const error = new ApiError(400, {type: '/errors/voting-deadline-not-in-future', field: 'votingDeadline'})

            const {container} = render(<ErrorDisplay error={error} fieldPrompts={{votingDeadline: 'Termín'}}/>)

            expect(container.firstChild).toBeNull()
        })

        it('shows a typed error bound to a field that is not in the form', () => {
            const error = new ApiError(400, {type: '/errors/voting-deadline-not-in-future', field: 'votingDeadline'})

            render(<ErrorDisplay error={error} fieldPrompts={{name: 'Název'}}/>)

            expect(screen.getByText('Termín pro výběr musí být v budoucnosti.')).toBeInTheDocument()
        })

        it('keeps the custom message ahead of field errors', () => {
            render(<ErrorDisplay error={validationError} customMessage="Opravte chyby"/>)

            expect(screen.getByText('Opravte chyby')).toBeInTheDocument()
            expect(screen.queryByText(labels.errors.formValidationErrors)).not.toBeInTheDocument()
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
        it('should display title, subtitle, field errors and buttons together', () => {
            const {container} = render(
                <ErrorDisplay
                    error={validationError}
                    title="Form Submission Error"
                    subtitle="Check your input fields"
                    onRetry={vi.fn()}
                    onCancel={vi.fn()}
                />
            )
            expect(screen.getByText('Form Submission Error')).toBeInTheDocument()
            expect(screen.getByText('Check your input fields')).toBeInTheDocument()
            expect(container.textContent).toContain('nesmí být prázdné')
            expect(screen.getByTestId('error-display-retry-button')).toBeInTheDocument()
            expect(screen.getByTestId('error-display-cancel-button')).toBeInTheDocument()
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
