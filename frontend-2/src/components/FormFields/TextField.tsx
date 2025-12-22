import {forwardRef} from 'react'
import clsx from 'clsx'
import {FieldWrapper} from './FieldWrapper'
import type {TextAreaFieldProps, TextFieldProps} from './types'

const baseInputClasses = clsx(
    'w-full px-4 py-2.5 border rounded-md font-normal',
    'text-text-primary',
    'bg-surface-base',
    'border-border',
    'placeholder-text-tertiary',
    'focus:outline-none focus:border-primary focus:ring-2 focus:ring-primary focus:ring-opacity-20',
    'focus:scale-y-105',
    'disabled:opacity-50 disabled:cursor-not-allowed',
    'transition-all duration-base'
)

const errorInputClasses = 'border-feedback-error focus:ring-feedback-error focus:ring-opacity-20 focus:border-feedback-error'

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
