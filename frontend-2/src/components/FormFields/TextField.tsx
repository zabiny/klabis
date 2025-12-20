import {forwardRef} from 'react'
import clsx from 'clsx'
import {FieldWrapper} from './FieldWrapper'
import type {TextAreaFieldProps, TextFieldProps} from './types'

const baseInputClasses = clsx(
    'w-full px-4 py-2.5 border rounded-lg font-normal',
    'text-gray-900 dark:text-white',
    'bg-white dark:bg-gray-700',
    'border-gray-300 dark:border-gray-600',
    'placeholder-gray-400 dark:placeholder-gray-500',
    'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0',
    'dark:focus:ring-offset-0',
    'disabled:opacity-50 disabled:cursor-not-allowed',
    'transition-colors duration-200'
)

const errorInputClasses = 'border-red-500 dark:border-red-400 focus:ring-red-500'

/**
 * TextField component for text input fields
 * Supports various input types: text, email, password, number, date, url, tel
 * Can also be used for textarea by setting multiline=true
 */
export const TextField = forwardRef<
    HTMLInputElement,
    TextFieldProps & { multiline?: false }
>(
    (
        {
            label,
            error,
            required,
            helpText,
            disabled,
            className,
            type = 'text',
            multiline = false,
            ...inputProps
        },
        ref
    ) => {
        const inputClasses = clsx(
            baseInputClasses,
            error && errorInputClasses,
            className
        )

        return (
            <FieldWrapper
                label={label}
                error={error}
                required={required}
                helpText={helpText}
            >
                <input
                    ref={ref}
                    type={type}
                    disabled={disabled}
                    className={inputClasses}
                    {...inputProps}
                />
            </FieldWrapper>
        )
    }
)

TextField.displayName = 'TextField'

/**
 * TextAreaField component for multiline text input
 */
export const TextAreaField = forwardRef<
    HTMLTextAreaElement,
    TextAreaFieldProps
>(
    (
        {
            label,
            error,
            required,
            helpText,
            disabled,
            className,
            rows = 4,
            ...textareaProps
        },
        ref
    ) => {
        const textareaClasses = clsx(baseInputClasses, error && errorInputClasses, className)

        return (
            <FieldWrapper
                label={label}
                error={error}
                required={required}
                helpText={helpText}
            >
        <textarea
            ref={ref}
            disabled={disabled}
            rows={rows}
            className={textareaClasses}
            {...textareaProps}
        />
            </FieldWrapper>
        )
    }
)

TextAreaField.displayName = 'TextAreaField'
