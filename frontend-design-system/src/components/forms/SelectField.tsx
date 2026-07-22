import {forwardRef} from 'react'
import clsx from 'clsx'
import {FieldWrapper} from './FieldWrapper.tsx'
import type {SelectFieldProps} from './types.ts'

const selectBaseClasses = clsx(
    'w-full px-3 py-1.5 border rounded-md font-normal text-sm',
    'text-text-primary',
    'bg-surface-raised',
    'border-border',
    'placeholder-text-tertiary',
    'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0',
    'disabled:opacity-50 disabled:cursor-not-allowed',
    'appearance-none',
    'transition-colors duration-200'
)

const errorSelectClasses = 'border-error focus:ring-error'

/**
 * SelectField component for dropdown selections
 * Supports single select and multiple select
 */
export const SelectField = forwardRef<HTMLSelectElement, SelectFieldProps>(
    (
        {
            label,
            error,
            required,
            helpText,
            disabled,
            className,
            options,
            placeholder,
            multiple = false,
            id,
            ...selectProps
        },
        ref
    ) => {
        const selectClasses = clsx(
            selectBaseClasses,
            error && errorSelectClasses,
            className,
            // Add right padding so the value never overlaps the arrow
            'pr-10'
        )

        return (
            <FieldWrapper
                label={label}
                error={error}
                required={required}
                helpText={helpText}
                id={id}
            >
                <div className="relative">
                    <select
                        ref={ref}
                        id={id}
                        disabled={disabled}
                        multiple={multiple}
                        className={selectClasses}
                        {...selectProps}
                    >
                        {placeholder && (
                            <option value="">
                                {placeholder}
                            </option>
                        )}
                        {options.map((option) => (
                            <option
                                key={`${option.value}`}
                                value={option.value}
                                disabled={option.disabled}
                            >
                                {option.label}
                            </option>
                        ))}
                    </select>

                    {/* Custom chevron, placed inside the field right after the value */}
                    {!disabled && (
                        <div
                            className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none text-text-tertiary">
                            <svg
                                className="w-4 h-4"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M6 9l6 6 6-6"
                                />
                            </svg>
                        </div>
                    )}
                </div>
            </FieldWrapper>
        )
    }
)

SelectField.displayName = 'SelectField'
