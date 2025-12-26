import clsx from 'clsx'
import {FieldWrapper} from './FieldWrapper'
import type {SwitchFieldProps} from './types'

/**
 * SwitchField component for boolean toggle input
 * Replaces MUI Switch
 */
export const SwitchField = ({
                                label,
                                error,
                                required,
                                helpText,
                                disabled,
                                className,
                                name,
                                checked,
                                onChange,
                            }: SwitchFieldProps) => {
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
                <div
                    className={clsx(
                        'relative inline-flex items-center h-6 rounded-full w-11',
                        'transition-colors duration-300',
                        'focus-within:outline focus-within:outline-2 focus-within:outline-primary focus-within:outline-offset-2',
                        disabled && 'opacity-50 cursor-not-allowed',
                        checked
                            ? 'bg-primary dark:bg-primary-dark'
                            : 'bg-gray-300 dark:bg-gray-600'
                    )}
                    role="switch"
                    aria-checked={checked}
                    aria-label={label || name}
                    aria-disabled={disabled}
                >
                    <input
                        type="checkbox"
                        name={name}
                        checked={checked}
                        onChange={handleChange}
                        disabled={disabled}
                        className="sr-only"
                    />
                    {/* Toggle knob */}
                    <div
                        className={clsx(
                            'inline-block h-4 w-4 transform rounded-full bg-white dark:bg-gray-200',
                            'transition-transform duration-300',
                            checked ? 'translate-x-6' : 'translate-x-1',
                            'shadow-md'
                        )}
                    />
                </div>
            </label>
        </FieldWrapper>
    )
}

SwitchField.displayName = 'SwitchField'
