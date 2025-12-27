import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {RadioGroup} from '../../FormFields'
import {useHalFormOptions} from '../../../hooks/useHalFormOptions'
import type {HalFormsInputProps} from '../types'

/**
 * HalFormsRadio component - radio group selection for HAL+Forms
 *
 * Uses Formik Field and FormFields RadioGroup abstraction.
 * Options are fetched via useHalFormOptions which handles both inline
 * and link-based options with automatic React Query caching.
 */
export const HalFormsRadio = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    const {options, isLoading} = useHalFormOptions(prop.options)

    return (
        <Field
            name={prop.name}
            validate={() => undefined}
            render={(fieldProps: FieldProps<unknown>) => {
                const fieldValue = fieldProps.field.value as string | number | undefined;
                return (
                    <RadioGroup
                        label={prop.prompt || prop.name}
                        name={prop.name}
                        required={prop.required}
                        disabled={prop.readOnly || isLoading || false}
                        error={errorText}
                        options={options}
                        value={fieldValue}
                        onChange={(value: string | number) => fieldProps.form.setFieldValue(prop.name, value)}
                        direction="vertical"
                    />
                );
            }}
        />
    )
}

HalFormsRadio.displayName = 'HalFormsRadio'
