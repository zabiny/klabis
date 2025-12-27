import clsx from 'clsx'
import {FieldWrapper} from './FieldWrapper'
import type {RadioGroupProps} from './types'

/**
 * RadioGroup component for exclusive selection among multiple options
 * Replaces MUI RadioGroup + FormControlLabel + Radio
 */
export const RadioGroup = ({
                               label,
                               error,
                               required,
                               helpText,
                               disabled,
                               className,
                               name,
                               options,
                               value,
                               onChange,
                               direction = 'vertical',
                           }: RadioGroupProps) => {
    const containerClasses = clsx(
        'flex gap-4',
        direction === 'horizontal' ? 'flex-row' : 'flex-col'
    )

    const handleChange = (optionValue: string | number) => {
        onChange?.(optionValue)
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
                            type="radio"
                            name={name}
                            value={option.value}
                            checked={value === option.value}
                            onChange={() => handleChange(option.value)}
                            disabled={disabled || option.disabled}
                            className={clsx(
                                'w-4 h-4 rounded-full',
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

RadioGroup.displayName = 'RadioGroup'
