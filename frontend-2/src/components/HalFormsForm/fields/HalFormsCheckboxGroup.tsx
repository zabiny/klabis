import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {CheckboxGroup} from '../../FormFields'
import {useHalFormOptions} from '../../../hooks/useHalFormOptions'
import type {HalFormsInputProps} from '../types'

/**
 * HalFormsCheckboxGroup component - multiple checkbox selection for HAL+Forms
 *
 * Uses Formik Field and FormFields CheckboxGroup abstraction.
 * Options are fetched via useHalFormOptions which handles both inline
 * and link-based options with automatic React Query caching.
 */
export const HalFormsCheckboxGroup = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    const {options, isLoading} = useHalFormOptions(prop.options)

    return (
        <Field
            name={prop.name}
            validate={() => undefined}
            render={(fieldProps: FieldProps<unknown>) => (
                <CheckboxGroup
                    label={prop.prompt || prop.name}
                    name={prop.name}
                    required={prop.required}
                    disabled={prop.readOnly || isLoading || false}
                    error={errorText}
                    options={options}
                    value={Array.isArray(fieldProps.field.value) ? fieldProps.field.value : []}
                    onChange={(value: (string | number)[]) => fieldProps.form.setFieldValue(prop.name, value)}
                    direction="vertical"
                />
            )}
        />
    )
}

HalFormsCheckboxGroup.displayName = 'HalFormsCheckboxGroup'
