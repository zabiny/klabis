import {type ReactElement, useEffect} from 'react'
import {Field, useFormikContext} from 'formik'
import {CheckboxGroup} from '../../../UI/forms'
import {useHalFormOptions} from '../../../../hooks/useHalFormOptions.ts'
import type {HalFormsInputProps} from '../types.ts'
import {getFieldLabel} from '../../../../localization'
import {normalizeArrayValue} from './normalizeArrayValue.ts'
import {needsNormalization, toSubmitValue} from './multiValueSubmit.ts'

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
