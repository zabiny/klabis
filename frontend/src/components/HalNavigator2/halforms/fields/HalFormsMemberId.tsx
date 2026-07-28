import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field, useField} from 'formik'
import {SelectField} from '../../../UI/forms'
import {useHalFormOptions} from '../../../../hooks/useHalFormOptions.ts'
import type {HalFormsInputProps} from '../types.ts'
import {getFieldLabel} from '../../../../localization'
import {ReadOnlyDisplay} from '../HalFormsForm.tsx'

interface HalFormsMemberIdProps extends HalFormsInputProps {
    /** Member IDs to exclude from the picker options */
    excludeIds?: string[];
    /** When provided, only these member IDs will be shown (whitelist — used for "promote to owner" where only current members are valid) */
    includeIds?: string[];
}

/**
 * HalFormsMemberId component - dropdown selection for member ID with clear button
 *
 * Displays a select dropdown for member selection with a clear button (X icon)
 * that appears only when a value is selected. Clicking the X clears the selection.
 * When readOnly, resolves the selected id to its display label from the same
 * options list (loaded via useHalFormOptions) and shows plain text instead of a
 * disabled dropdown.
 */
export const HalFormsMemberId = ({prop, errorText, renderMode = 'field', excludeIds, includeIds}: HalFormsMemberIdProps): ReactElement => {
    const {options: rawOptions, isLoading} = useHalFormOptions(prop.options)
    const [field] = useField<unknown>(prop.name)

    const options = rawOptions.filter(opt => {
        const id = String(opt.value);
        if (includeIds !== undefined) return includeIds.includes(id);
        if (excludeIds !== undefined) return !excludeIds.includes(id);
        return true;
    });

    if (prop.readOnly) {
        const fieldValue = field.value as string | number | undefined;
        const selectedLabel = fieldValue !== undefined && fieldValue !== null && fieldValue !== ''
            ? (options.find(opt => opt.value === String(fieldValue))?.label ?? (isLoading ? 'Načítání...' : '—'))
            : '—';
        return <ReadOnlyDisplay label={prop.prompt || getFieldLabel(prop.name)} value={selectedLabel} renderMode={renderMode} />;
    }

    return (
        <Field name={prop.name} validate={() => undefined}>
            {({field}: FieldProps<unknown>) => {
                const fieldValue = field.value as string | number | undefined;
                // Convert undefined/null to empty string so it matches placeholder's empty value
                const selectValue = (fieldValue === undefined || fieldValue === null || fieldValue === '') ? '' : fieldValue;
                const hasValue = selectValue !== '';

                const handleClear = () => {
                    field.onChange({
                        target: {
                            name: prop.name,
                            value: ''
                        }
                    } as React.ChangeEvent<HTMLSelectElement>);
                };

                return (
                    <div className="relative">
                        <SelectField
                            {...field}
                            id={`field-${prop.name}`}
                            value={selectValue}
                            label={renderMode === 'field' ? (prop.prompt || getFieldLabel(prop.name)) : undefined}
                            placeholder={isLoading ? 'Načítání...' : 'Vyberte možnost'}
                            disabled={isLoading}
                            required={prop.required}
                            error={errorText}
                            options={options}
                            className="w-full"
                        />
                        {/* Clear button - only visible when value is selected */}
                        {hasValue && (
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
        </Field>
    )
}

HalFormsMemberId.displayName = 'HalFormsMemberId'
