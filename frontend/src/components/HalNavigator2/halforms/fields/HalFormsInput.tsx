import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {TextField} from '../../../UI/forms'
import type {HalFormsInputProps} from '../types.ts'

/**
 * HalFormsInput component - text input for HAL+Forms
 * Replaces MUI TextField for text, email, number, date types
 * Uses Formik Field and FormFields TextField abstraction
 */
export const HalFormsInput = ({
                                  prop,
                                  errorText,
                                  renderMode = 'field',
                              }: HalFormsInputProps): ReactElement => {
    return (
        <Field name={prop.name} validate={() => undefined}>
            {({field}: FieldProps<unknown>) => {
                const fieldValue = field.value as string | number | undefined;
                return (
                    <TextField
                        {...field}
                        value={fieldValue}
                        type={(prop.type as 'text' | 'email' | 'password' | 'number' | 'date' | 'datetime-local' | 'url' | 'tel') || 'text'}
                        label={renderMode === 'field' ? (prop.prompt || prop.name) : undefined}
                        disabled={prop.readOnly || false}
                        required={prop.required}
                        error={errorText}
                        className="w-full"
                    />
                );
            }}
        </Field>
    )
}

HalFormsInput.displayName = 'HalFormsInput'
