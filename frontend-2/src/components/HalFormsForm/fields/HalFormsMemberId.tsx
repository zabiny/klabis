import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {SelectField} from '../../FormFields'
import {useHalFormOptions} from '../../../hooks/useHalFormOptions'
import type {HalFormsInputProps} from '../types'

/**
 * HalFormsMemberId component - dropdown selection for member ID with clear button
 *
 * Displays a select dropdown for member selection with a clear button (X icon)
 * that appears only when a value is selected. Clicking the X clears the selection.
 */
export const HalFormsMemberId = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    const {options, isLoading} = useHalFormOptions(prop.options)

    return (
        <Field
            name={prop.name}
            validate={() => undefined}
            render={(fieldProps: FieldProps<unknown>) => {
                const fieldValue = fieldProps.field.value as string | number | undefined;
                // Convert undefined/null to empty string so it matches placeholder's empty value
                const selectValue = (fieldValue === undefined || fieldValue === null || fieldValue === '') ? '' : fieldValue;
                const hasValue = selectValue !== '';

                const handleClear = () => {
                    fieldProps.field.onChange({
                        target: {
                            name: prop.name,
                            value: ''
                        }
                    } as React.ChangeEvent<HTMLSelectElement>);
                };

                return (
                    <div className="relative">
                        <SelectField
                            {...fieldProps.field}
                            id={`field-${prop.name}`}
                            value={selectValue}
                            label={prop.prompt || prop.name}
                            placeholder={isLoading ? 'Načítání...' : 'Vyberte možnost'}
                            disabled={prop.readOnly || isLoading || false}
                            required={prop.required}
                            error={errorText}
                            options={options}
                            className="w-full"
                        />
                        {/* Clear button - only visible when value is selected */}
                        {hasValue && !prop.readOnly && (
                            <button
                                type="button"
                                onClick={handleClear}
                                className="absolute right-20 top-1/2 transform -translate-y-1/2 text-text-secondary hover:text-text-primary transition-colors"
                                title="Zrušit výběr"
                                aria-label="Zrušit výběr"
                                data-testid="clear-member-button"
                            >
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
                                        d="M6 18L18 6M6 6l12 12"
                                    />
                                </svg>
                            </button>
                        )}
                    </div>
                );
            }}
        />
    )
}

HalFormsMemberId.displayName = 'HalFormsMemberId'
