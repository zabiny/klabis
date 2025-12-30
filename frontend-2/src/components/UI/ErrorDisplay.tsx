import type {ReactElement} from 'react'
import {Alert} from './Alert'
import {isFormValidationError} from '../../api/hateoas'

/**
 * Props for ErrorDisplay component
 */
export interface ErrorDisplayProps {
    /** Error object (FetchError, FormValidationError, or any Error) */
    error: Error | null | undefined
    /** Custom title for the error alert */
    title?: string
    /** Custom message to display instead of error.message */
    customMessage?: string
    /** Callback when retry button is clicked */
    onRetry?: () => void
    /** Callback when cancel button is clicked */
    onCancel?: () => void
    /** Text for retry button (default: 'Retry') */
    retryText?: string
    /** Text for cancel button (default: 'Cancel') */
    cancelText?: string
    /** Optional subtitle or endpoint information */
    subtitle?: string
    /** CSS class name for additional styling */
    className?: string
}

/**
 * Reusable error display component for HAL navigation components
 * Consolidates error handling patterns from HalFormDisplay and HalEmbeddedTable
 *
 * @example
 * // Basic error display
 * <ErrorDisplay
 *   error={fetchError}
 *   title="Load Error"
 *   customMessage="Failed to load data"
 * />
 *
 * @example
 * // With retry and cancel actions
 * <ErrorDisplay
 *   error={targetFetchError}
 *   title="Form Data Load Error"
 *   onRetry={() => refetchTargetData()}
 *   onCancel={() => onClose()}
 * />
 *
 * @example
 * // With form validation errors
 * <ErrorDisplay
 *   error={submitError}
 *   title="Submission Failed"
 * />
 */
export function ErrorDisplay({
                                 error,
                                 title,
                                 customMessage,
                                 onRetry,
                                 onCancel,
                                 retryText = 'Retry',
                                 cancelText = 'Cancel',
                                 subtitle,
                                 className,
                             }: ErrorDisplayProps): ReactElement | null {
    if (!error) {
        return null
    }

    const errorMessage = customMessage || error.message
    const showValidationErrors = isFormValidationError(error)

    return (
        <Alert severity="error" className={className}>
            <div className="space-y-2">
                {title && (
                    <p className="font-semibold">{title}</p>
                )}
                <p>{errorMessage}</p>
                {subtitle && (
                    <p className="text-sm text-text-secondary">
                        {subtitle}
                    </p>
                )}

                {/* Display validation field errors if applicable */}
                {showValidationErrors && (
                    <ul className="list-disc list-inside text-sm">
                        {Object.entries(error.validationErrors).map(([field, fieldError]) => (
                            <li key={field}>
                                {field}: {fieldError}
                            </li>
                        ))}
                    </ul>
                )}

                {/* Display action buttons if callbacks provided */}
                {(onRetry || onCancel) && (
                    <div className="flex gap-2 pt-1">
                        {onRetry && (
                            <button
                                onClick={onRetry}
                                className="text-sm px-3 py-1 bg-primary text-white rounded hover:bg-primary-dark transition-colors"
                                data-testid="error-display-retry-button"
                            >
                                {retryText}
                            </button>
                        )}
                        {onCancel && (
                            <button
                                onClick={onCancel}
                                className="text-sm px-3 py-1 bg-gray-300 text-gray-900 rounded hover:bg-gray-400 transition-colors"
                                data-testid="error-display-cancel-button"
                            >
                                {cancelText}
                            </button>
                        )}
                    </div>
                )}
            </div>
        </Alert>
    )
}

ErrorDisplay.displayName = 'ErrorDisplay'
