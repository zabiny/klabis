import type {ReactNode} from 'react'
import clsx from 'clsx'

interface FieldWrapperInternalProps {
    label?: string
    error?: string
    required?: boolean
    helpText?: string
    children: ReactNode
    className?: string
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
                             }: FieldWrapperInternalProps) => {
    return (
        <div className={clsx('w-full', className)}>
            {label && (
                <label className="block text-sm font-medium text-gray-900 dark:text-gray-100 mb-2">
                    {label}
                    {required && <span className="text-red-500 dark:text-red-400 ml-1">*</span>}
                </label>
            )}

            {/* Field content */}
            <div className={clsx('relative', error && 'mb-1')}>
                {children}
            </div>

            {/* Error message */}
            {error && (
                <p className="mt-1 text-sm text-red-600 dark:text-red-400 font-medium">
                    {error}
                </p>
            )}

            {/* Help text (only shown if no error) */}
            {helpText && !error && (
                <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                    {helpText}
                </p>
            )}
        </div>
    )
}
