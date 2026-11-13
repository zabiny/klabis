import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {RadioGroup} from '../../../UI/forms'
import {useHalFormOptions} from '../../../../hooks/useHalFormOptions.ts'
import type {HalFormsInputProps} from '../types.ts'
import {fieldLabel} from './fieldLabel.ts'

/**
 * HalFormsRadio component - radio group selection for HAL+Forms
 *
 * Uses Formik Field and FormFields RadioGroup abstraction.
 * Options are fetched via useHalFormOptions which handles both inline
 * and link-based options with automatic React Query caching.
 */
export const HalFormsRadio = ({prop, errorText, renderMode = 'field'}: HalFormsInputProps): ReactElement => {
    const {options, isLoading} = useHalFormOptions(prop.options, prop)

    return (
        <Field name={prop.name} validate={() => undefined}>
            {({field, form}: FieldProps<unknown>) => {
                const fieldValue = field.value as string | number | undefined;
                return (
                    <RadioGroup
                        label={renderMode === 'field' ? fieldLabel(prop) : undefined}
                        groupAriaLabel={renderMode === 'input' ? fieldLabel(prop) : undefined}
                        name={prop.name}
                        required={prop.required}
                        disabled={prop.readOnly || isLoading || false}
                        error={errorText}
                        options={options}
                        value={fieldValue}
                        onChange={(value: string | number) => form.setFieldValue(prop.name, value)}
                        direction="vertical"
                    />
                );
            }}
        </Field>
    )
}

HalFormsRadio.displayName = 'HalFormsRadio'
