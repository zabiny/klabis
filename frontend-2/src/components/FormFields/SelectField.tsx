import {forwardRef} from 'react'
import clsx from 'clsx'
import {FieldWrapper} from './FieldWrapper'
import type {SelectFieldProps} from './types'

const selectBaseClasses = clsx(
    'w-full px-4 py-2.5 border rounded-lg font-normal',
    'text-gray-900 dark:text-white',
    'bg-white dark:bg-gray-700',
    'border-gray-300 dark:border-gray-600',
    'placeholder-gray-400 dark:placeholder-gray-500',
    'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0',
    'dark:focus:ring-offset-0',
    'disabled:opacity-50 disabled:cursor-not-allowed',
    'appearance-none',
    'transition-colors duration-200'
)

const errorSelectClasses = 'border-red-500 dark:border-red-400 focus:ring-red-500'

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
            ...selectProps
        },
        ref
    ) => {
        const selectClasses = clsx(
            selectBaseClasses,
            error && errorSelectClasses,
            className,
            // Add right padding for dropdown arrow
            'pr-10',
            // Add background image for custom arrow
            'bg-no-repeat',
            'bg-right-4',
            'bg-[length:20px_20px]'
        )

        // Simple inline SVG for dropdown arrow
        const arrowSvg = `data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 20 20' fill='%23666'%3E%3Cpath fill-rule='evenodd' d='M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z' clip-rule='evenodd'/%3E%3C/svg%3E`

        const selectStyleWithArrow = {
            ...selectProps.style,
            backgroundImage: `url('${arrowSvg}')`,
        } as React.CSSProperties

        return (
            <FieldWrapper
                label={label}
                error={error}
                required={required}
                helpText={helpText}
            >
                <div className="relative">
                    <select
                        ref={ref}
                        disabled={disabled}
                        multiple={multiple}
                        className={selectClasses}
                        style={selectStyleWithArrow}
                        {...selectProps}
                    >
                        {placeholder && (
                            <option value="" disabled>
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

                    {/* Custom arrow for non-disabled state */}
                    {!disabled && (
                        <div
                            className="absolute right-4 top-1/2 transform -translate-y-1/2 pointer-events-none text-gray-400 dark:text-gray-500">
                            <svg
                                className="w-5 h-5"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                            >
                                <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M19 14l-7 7m0 0l-7-7m7 7V3"
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
