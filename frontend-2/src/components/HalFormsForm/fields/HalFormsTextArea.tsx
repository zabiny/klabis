import type {ReactElement} from 'react'
import type {FieldProps} from 'formik'
import {Field} from 'formik'
import {TextAreaField} from '../../FormFields'
import type {HalFormsInputProps} from '../types'

/**
 * HalFormsTextArea component - multiline textarea for HAL+Forms
 * Uses Formik Field and FormFields TextAreaField abstraction
 */
export const HalFormsTextArea = ({
                                     prop,
                                     errorText,
                                 }: HalFormsInputProps): ReactElement => {
    return (
        <Field
            name={prop.name}
            validate={() => undefined}
            render={(fieldProps: FieldProps<unknown>) => {
                const fieldValue = fieldProps.field.value as string | undefined;
                return (
                    <TextAreaField
                        {...fieldProps.field}
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
        />
    )
}

HalFormsTextArea.displayName = 'HalFormsTextArea'
