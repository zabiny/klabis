import type {ReactElement} from 'react'
import {Alert} from './Alert'
import {Button} from './Button'
import {ApiError, VALIDATION_FAILED} from '../../api/apiError'
import {describeError, labels} from '../../localization'
import {errorStyles, layoutStyles} from '../../theme/designTokens'

/**
 * Props for ErrorDisplay component
 */
export interface ErrorDisplayProps {
    /** Error object (ApiError, or any Error) */
    error: Error | null | undefined
    /** Custom title for the error alert */
    title?: string
    /** Custom message to display instead of the described error */
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
    /** Prompts of the form's template properties by name; errors bound to these are shown at their fields, not here */
    fieldPrompts?: Record<string, string>
}

interface FormErrorContent {
    message?: string
    errors: string[]
    unmatchedFieldErrors: Array<{key: string, text: string}>
    hasFieldErrors: boolean
}

function describeFormError(error: ApiError, fieldPrompts: Record<string, string>): FormErrorContent {
    const isFormField = (key: string) => Object.hasOwn(fieldPrompts, key.split('.')[0])
    const isBoundToField = error.field !== undefined && Object.hasOwn(fieldPrompts, error.field)
    const hasFieldErrors = Object.keys(error.fieldErrors).length > 0
    const detailsShown = hasFieldErrors || error.errors.length > 0
    const hidesGenericMessage = error.type === VALIDATION_FAILED && detailsShown
    const unmatchedFieldErrors = Object.entries(error.fieldErrors)
        .filter(([key]) => !isFormField(key))
        .map(([key, text]) => ({key, text}))
    return {
        message: isBoundToField || hidesGenericMessage ? undefined : describeError(error),
        errors: error.errors,
        unmatchedFieldErrors,
        hasFieldErrors,
    }
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
 *   fieldPrompts={{name: 'Název'}}
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
                                 fieldPrompts = {},
                             }: ErrorDisplayProps): ReactElement | null {
    if (!error) {
        return null
    }

    const apiError = error instanceof ApiError ? error : undefined
    const content = apiError ? describeFormError(apiError, fieldPrompts) : undefined
    const errorMessage = customMessage || (content ? content.message : describeError(error))
    const heading = !errorMessage && content?.hasFieldErrors ? labels.errors.formValidationErrors : undefined
    const hasContent = Boolean(
        title || errorMessage || heading || subtitle || onRetry || onCancel
        || content?.errors.length || content?.unmatchedFieldErrors.length
    )

    if (!hasContent) {
        return null
    }

    return (
        <Alert severity="error" className={className}>
            <div className={layoutStyles.verticalStack}>
                {title && (
                    <p className="font-semibold">{title}</p>
                )}
                {errorMessage && <p>{errorMessage}</p>}
                {heading && <p>{heading}</p>}
                {subtitle && (
                    <p className={errorStyles.validationMessage}>
                        {subtitle}
                    </p>
                )}

                {content && (content.errors.length > 0 || content.unmatchedFieldErrors.length > 0) && (
                    <ul className={errorStyles.listItem}>
                        {content.errors.map((text, index) => (
                            <li key={`error-${index}`}>{text}</li>
                        ))}
                        {content.unmatchedFieldErrors.map(({key, text}) => (
                            <li key={key}>{text}</li>
                        ))}
                    </ul>
                )}

                {/* Display action buttons if callbacks provided */}
                {(onRetry || onCancel) && (
                    <div className={layoutStyles.formControls}>
                        {onRetry && (
                            <Button
                                variant="primary"
                                size="sm"
                                onClick={onRetry}
                                data-testid="error-display-retry-button"
                            >
                                {retryText}
                            </Button>
                        )}
                        {onCancel && (
                            <Button
                                variant="secondary"
                                size="sm"
                                onClick={onCancel}
                                data-testid="error-display-cancel-button"
                            >
                                {cancelText}
                            </Button>
                        )}
                    </div>
                )}
            </div>
        </Alert>
    )
}

ErrorDisplay.displayName = 'ErrorDisplay'
