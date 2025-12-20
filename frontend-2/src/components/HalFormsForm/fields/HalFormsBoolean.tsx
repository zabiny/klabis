import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {SwitchField} from '../../FormFields'
import type {HalFormsInputProps} from '../types'

/**
 * HalFormsBoolean component - switch/toggle for HAL+Forms boolean values
 * Uses Formik Field and FormFields SwitchField abstraction
 */
export const HalFormsBoolean = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    return (
        <Field
            name={prop.name}
            type="checkbox"
            validate={() => undefined}
            render={(fieldProps: FieldProps<unknown>) => (
                <SwitchField
                    name={prop.name}
                    label={prop.prompt || prop.name}
                    required={prop.required}
                    disabled={prop.readOnly || false}
                    error={errorText}
                    checked={Boolean(fieldProps.field.value)}
                    onChange={(checked: boolean) => fieldProps.form.setFieldValue(prop.name, checked)}
                />
            )}
        />
    )
}

HalFormsBoolean.displayName = 'HalFormsBoolean'
