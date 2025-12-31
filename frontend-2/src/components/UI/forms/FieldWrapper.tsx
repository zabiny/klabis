import type {ReactNode} from 'react'
import clsx from 'clsx'

interface FieldWrapperInternalProps {
    label?: string
    error?: string
    required?: boolean
    helpText?: string
    children: ReactNode
    className?: string
    id?: string
}

/**
 * FieldWrapper provides consistent layout and styling for all form fields.
 * It handles label, error messages, and help text in a consistent way.
 *
 * This is the core pattern that ensures all form fields have a consistent appearance
 * and behavior across the application.
 */
export const FieldWrapper = ({
                                 label,
                                 error,
                                 required,
                                 helpText,
                                 children,
                                 className,
                                 id,
                             }: FieldWrapperInternalProps) => {
    return (
        <div className={clsx('w-full', className)}>
            {label && (
                <label htmlFor={id}
                       className="block text-xs font-semibold text-text-secondary mb-2 uppercase tracking-wider">
                    {label}
                    {required && <span className="text-feedback-error ml-1">*</span>}
                </label>
            )}

            {/* Field content */}
            <div className={clsx('relative', error && 'mb-1')}>
                {children}
            </div>

            {/* Error message */}
            {error && (
                <p className="mt-1 text-sm text-feedback-error font-medium animate-fade-in">
                    {error}
                </p>
            )}

            {/* Help text (only shown if no error) */}
            {helpText && !error && (
                <p className="mt-1 text-sm text-text-tertiary">
                    {helpText}
                </p>
            )}
        </div>
    )
}
