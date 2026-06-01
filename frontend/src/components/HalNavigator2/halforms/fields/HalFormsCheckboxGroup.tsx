import {type ReactElement, useEffect} from 'react'
import {Field, useFormikContext} from 'formik'
import {CheckboxGroup} from '../../../UI/forms'
import {useHalFormOptions} from '../../../../hooks/useHalFormOptions.ts'
import type {HalFormsInputProps} from '../types.ts'
import {getFieldLabel} from '../../../../localization'

/**
 * Extracts a scalar value from a form array element as a string.
 *
 * When a HAL resource contains a List field (e.g. trainers), the API returns
 * full embedded objects like {memberId: "uuid", _links: {...}}. The checkbox
 * group options use string values for UI comparison, so raw objects must be
 * normalized to their scalar string ID before comparison or display.
 */
export function normalizeArrayValue(item: unknown): string {
    if (typeof item === 'number') {
        return String(item);
    }
    if (typeof item === 'string') {
        return item;
    }
    if (item !== null && typeof item === 'object') {
        const obj = item as Record<string, unknown>;
        if (typeof obj.memberId === 'string') return obj.memberId;
        if (typeof obj.value === 'string') return obj.value;
        if (typeof obj.value === 'number') return String(obj.value);
    }
    return String(item);
}

/**
 * Converts a string checkbox value to the correct submission type based on the HAL property type.
 *
 * For number-typed properties (e.g. orisDisciplineIds: Set<Integer> on backend),
 * values must be submitted as numbers — Jackson cannot deserialize string "1" into Integer.
 * For all other types (text, UUID-based trainers), string submission is correct.
 */
function toSubmitValue(value: string, propType: string): string | number {
    if (propType === 'number') {
        return Number(value);
    }
    return value;
}

/**
 * Checks whether the array contains elements that need object/number → string normalization for UI display.
 * Pure number[] where the prop type is 'number' does NOT need normalization — they are already the correct
 * submit type and normalizeArrayValue handles string conversion for UI matching on the fly.
 */
function needsNormalization(arr: unknown[], propType: string): boolean {
    return arr.some((item) => {
        if (item !== null && typeof item === 'object') return true;
        // number items in a non-number-typed field need string normalization
        if (typeof item === 'number' && propType !== 'number') return true;
        return false;
    });
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
        // Only normalize HAL objects (e.g. trainer {memberId, _links}) or number values in non-number fields.
        // number[] for number-typed fields is already the correct submit type — no normalization needed.
        if (needsNormalization(valueArray, prop.type)) {
            setFieldValue(prop.name, valueArray.map(normalizeArrayValue));
        }
    }, [prop.name]);  // intentionally only on mount — normalizes stale HAL objects from initial data

    // UI display uses string comparison so number 1 matches string option "1"
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
            onChange={(value: (string | number)[]) =>
                setFieldValue(prop.name, value.map(String).map((v) => toSubmitValue(v, prop.type)))
            }
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
