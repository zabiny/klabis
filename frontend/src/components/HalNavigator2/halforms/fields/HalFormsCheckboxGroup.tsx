import {type ReactElement, useEffect} from 'react'
import {Field, useFormikContext} from 'formik'
import {CheckboxGroup} from '../../../UI/forms'
import {useHalFormOptions} from '../../../../hooks/useHalFormOptions.ts'
import type {HalFormsInputProps} from '../types.ts'
import {getFieldLabel} from '../../../../localization'

/**
 * Extracts a scalar value from a form array element.
 *
 * When a HAL resource contains a List field (e.g. trainers), the API returns
 * full embedded objects like {memberId: "uuid", _links: {...}}. The checkbox
 * group options use UUID strings as values, so raw objects must be normalized
 * to their scalar ID before comparison or submission.
 */
export function normalizeArrayValue(item: unknown): string | number {
    if (typeof item === 'string' || typeof item === 'number') {
        return item;
    }
    if (item !== null && typeof item === 'object') {
        const obj = item as Record<string, unknown>;
        if (typeof obj.memberId === 'string') return obj.memberId;
        if (typeof obj.value === 'string' || typeof obj.value === 'number') return obj.value as string | number;
    }
    return String(item);
}

function needsNormalization(arr: unknown[]): boolean {
    return arr.some((item) => item !== null && typeof item === 'object');
}

interface CheckboxGroupFieldProps {
    prop: HalFormsInputProps['prop'];
    errorText?: string;
    renderMode: 'field' | 'input';
    options: {value: string | number; label: string}[];
    isLoading: boolean;
}

const CheckboxGroupField = ({prop, errorText, renderMode, options, isLoading}: CheckboxGroupFieldProps): ReactElement => {
    const {values, setFieldValue} = useFormikContext<Record<string, unknown>>();
    const rawValue = values[prop.name];
    const valueArray = Array.isArray(rawValue) ? rawValue : [];

    useEffect(() => {
        if (needsNormalization(valueArray)) {
            setFieldValue(prop.name, valueArray.map(normalizeArrayValue));
        }
    }, [prop.name]);  // intentionally only on mount — normalizes stale HAL objects from initial data

    const normalizedValue = valueArray.map(normalizeArrayValue);

    return (
        <CheckboxGroup
            label={renderMode === 'field' ? (prop.prompt || getFieldLabel(prop.name)) : undefined}
            name={prop.name}
            required={prop.required}
            disabled={prop.readOnly || isLoading || false}
            error={errorText}
            options={options}
            value={normalizedValue}
            onChange={(value: (string | number)[]) => setFieldValue(prop.name, value)}
            direction="vertical"
        />
    );
};

/**
 * HalFormsCheckboxGroup component - multiple checkbox selection for HAL+Forms
 *
 * Uses Formik Field and FormFields CheckboxGroup abstraction.
 * Options are fetched via useHalFormOptions which handles both inline
 * and link-based options with automatic React Query caching.
 */
export const HalFormsCheckboxGroup = ({prop, errorText, renderMode = 'field'}: HalFormsInputProps): ReactElement => {
    const {options, isLoading} = useHalFormOptions(prop.options)

    return (
        <Field name={prop.name} validate={() => undefined}>
            {() => (
                <CheckboxGroupField
                    prop={prop}
                    errorText={errorText}
                    renderMode={renderMode}
                    options={options}
                    isLoading={isLoading}
                />
            )}
        </Field>
    )
}

HalFormsCheckboxGroup.displayName = 'HalFormsCheckboxGroup'
