import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {CheckboxField} from '../../../UI/forms'
import type {HalFormsInputProps} from '../types.ts'

/**
 * HalFormsCheckbox component - single checkbox for HAL+Forms
 * Uses Formik Field and FormFields CheckboxField abstraction
 */
export const HalFormsCheckbox = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    return (
        <Field name={prop.name} type="checkbox" validate={() => undefined}>
            {({field, form}: FieldProps<unknown>) => (
                <CheckboxField
                    name={prop.name}
                    label={prop.prompt || prop.name}
                    required={prop.required}
                    disabled={prop.readOnly || false}
                    error={errorText}
                    checked={Boolean(field.value)}
                    onChange={(checked: boolean) => form.setFieldValue(prop.name, checked)}
                />
            )}
        </Field>
    )
}

HalFormsCheckbox.displayName = 'HalFormsCheckbox'
