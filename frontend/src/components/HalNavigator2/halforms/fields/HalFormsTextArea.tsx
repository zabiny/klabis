import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {TextAreaField} from '../../../UI/forms'
import type {HalFormsInputProps} from '../types.ts'

/**
 * HalFormsTextArea component - multiline textarea for HAL+Forms
 * Uses Formik Field and FormFields TextAreaField abstraction
 */
export const HalFormsTextArea = ({
                                     prop,
                                     errorText,
                                 }: HalFormsInputProps): ReactElement => {
    return (
        <Field name={prop.name} validate={() => undefined}>
            {({field}: FieldProps<unknown>) => {
                const fieldValue = field.value as string | undefined;
                return (
                    <TextAreaField
                        {...field}
                        value={fieldValue}
                        label={prop.prompt || prop.name}
                        disabled={prop.readOnly || false}
                        required={prop.required}
                        error={errorText}
                        rows={4}
                        className="w-full"
                    />
                );
            }}
        </Field>
    )
}

HalFormsTextArea.displayName = 'HalFormsTextArea'
