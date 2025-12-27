import {forwardRef} from 'react'
import clsx from 'clsx'
import {FieldWrapper} from './FieldWrapper'
import type {CheckboxFieldProps, CheckboxGroupProps} from './types'

/**
 * CheckboxField component for single checkbox input
 * Replaces MUI Checkbox
 */
export const CheckboxField = forwardRef<HTMLInputElement, CheckboxFieldProps>(
    (
        {
            label,
            error,
            required,
            helpText,
            disabled,
            className,
            checked,
            onChange,
            ...inputProps
        },
        ref
    ) => {
        const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
            onChange?.(e.target.checked)
        }

        return (
            <FieldWrapper
                label={label}
                error={error}
                required={required}
                helpText={helpText}
                className={className}
            >
                <label className="flex items-center cursor-pointer group">
                    <input
                        ref={ref}
                        type="checkbox"
                        checked={checked}
                        onChange={handleChange}
                        disabled={disabled}
                        className={clsx(
                            'w-4 h-4 rounded',
                            'border-2 border-border',
                            'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0',
                            'disabled:opacity-50 disabled:cursor-not-allowed',
                            'cursor-pointer',
                            'accent-primary',
                            'transition-colors duration-200'
                        )}
                        {...inputProps}
                    />
                </label>
            </FieldWrapper>
        )
    }
)

CheckboxField.displayName = 'CheckboxField'

/**
 * CheckboxGroup component for multiple checkbox selection
 * Replaces MUI CheckboxGroup (FormGroup + multiple Checkboxes)
 */
export const CheckboxGroup = ({
                                  label,
                                  error,
                                  required,
                                  helpText,
                                  disabled,
                                  className,
                                  name,
                                  options,
                                  value = [],
                                  onChange,
                                  direction = 'vertical',
                              }: CheckboxGroupProps) => {
    const containerClasses = clsx(
        'flex gap-4',
        direction === 'horizontal' ? 'flex-row' : 'flex-col'
    )

    const handleChange = (optionValue: string | number, checked: boolean) => {
        const newValue = checked
            ? [...(value || []), optionValue]
            : (value || []).filter((v) => v !== optionValue)
        onChange?.(newValue)
    }

    return (
        <FieldWrapper
            label={label}
            error={error}
            required={required}
            helpText={helpText}
            className={className}
        >
            <div className={containerClasses} role="group" aria-labelledby={label ? `${name}-label` : undefined}>
                {options.map((option) => (
                    <label
                        key={`${option.value}`}
                        className="flex items-center cursor-pointer group"
                    >
                        <input
                            type="checkbox"
                            name={name}
                            value={option.value}
                            checked={(value || []).includes(option.value)}
                            onChange={(e) => handleChange(option.value, e.target.checked)}
                            disabled={disabled || option.disabled}
                            className={clsx(
                                'w-4 h-4 rounded',
                                'border-2 border-border',
                                'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-0',
                                'disabled:opacity-50 disabled:cursor-not-allowed',
                                'cursor-pointer',
                                'accent-primary',
                                'transition-colors duration-200'
                            )}
                            aria-label={option.label}
                        />
                        <span
                            className="ml-3 text-sm text-text-secondary group-hover:text-text-primary">
              {option.label}
            </span>
                    </label>
                ))}
            </div>
        </FieldWrapper>
    )
}

CheckboxGroup.displayName = 'CheckboxGroup'
