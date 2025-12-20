import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {TextField} from '../../FormFields'
import type {HalFormsInputProps} from '../types'

/**
 * HalFormsInput component - text input for HAL+Forms
 * Replaces MUI TextField for text, email, number, date types
 * Uses Formik Field and FormFields TextField abstraction
 */
export const HalFormsInput = ({
                                  prop,
                                  errorText,
                              }: HalFormsInputProps): ReactElement => {
    return (
        <Field
            name={prop.name}
            validate={() => undefined}
            render={(fieldProps: FieldProps<unknown>) => {
                const fieldValue = fieldProps.field.value as string | number | undefined;
                return (
                    <TextField
                        {...fieldProps.field}
                        value={fieldValue}
                        type={(prop.type as 'text' | 'email' | 'password' | 'number' | 'date' | 'url' | 'tel') || 'text'}
                        label={prop.prompt || prop.name}
                        disabled={prop.readOnly || false}
                        required={prop.required}
                        error={errorText}
                        className="w-full"
                    />
                );
            }}
        />
    )
}

HalFormsInput.displayName = 'HalFormsInput'
