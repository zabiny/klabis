import {type ReactElement, useState} from 'react'
import type {FieldProps} from 'formik'
import {Field, useField} from 'formik'
import {SelectField, TextField} from '../../../UI/forms'
import {useHalFormOptions} from '../../../../hooks/useHalFormOptions.ts'
import type {HalFormsInputProps} from '../types.ts'
import {getFieldLabel, labels} from '../../../../localization'
import {ReadOnlyDisplay} from '../HalFormsForm.tsx'
import {ClearSelectionButton} from './ClearSelectionButton.tsx'

interface HalFormsMemberIdProps extends HalFormsInputProps {
    /** Member IDs to exclude from the picker options */
    excludeIds?: string[];
    /** When provided, only these member IDs will be shown (whitelist — used for "promote to owner" where only current members are valid) */
    includeIds?: string[];
}

const normalize = (text: string): string =>
    text.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase();

const matchesSearch = (label: string, query: string): boolean => {
    const haystack = normalize(label);
    return normalize(query).split(/\s+/).filter(Boolean).every(word => haystack.includes(word));
};

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
    const [search, setSearch] = useState('')

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
                const visibleOptions = options.filter(opt =>
                    String(opt.value) === String(selectValue) || matchesSearch(opt.label, search));

                const handleClear = () => {
                    field.onChange({
                        target: {
                            name: prop.name,
                            value: ''
                        }
                    } as React.ChangeEvent<HTMLSelectElement>);
                };

                return (
                    <div className="space-y-2">
                        <TextField
                            type="text"
                            aria-label={labels.ui.optionSearch}
                            placeholder={labels.ui.optionSearch}
                            value={search}
                            onChange={event => setSearch(event.target.value)}
                        />
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
                            options={visibleOptions}
                            className="w-full"
                        />
                        {/* Clear button - only visible when value is selected */}
                        {hasValue && (
                            <ClearSelectionButton onClick={handleClear}/>
                        )}
                        </div>
                    </div>
                );
            }}
        </Field>
    )
}

HalFormsMemberId.displayName = 'HalFormsMemberId'
