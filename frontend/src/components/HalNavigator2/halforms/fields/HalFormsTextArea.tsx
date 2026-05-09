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
                                     renderMode = 'field',
                                 }: HalFormsInputProps): ReactElement => {
    return (
        <Field name={prop.name} validate={() => undefined}>
            {({field}: FieldProps<unknown>) => {
                const fieldValue = field.value as string | undefined;
                const currentLength = fieldValue?.length ?? 0;
                const maxLength = prop.maxLength;
                return (
                    <div>
                        <TextAreaField
                            {...field}
                            value={fieldValue}
                            label={renderMode === 'field' ? (prop.prompt || prop.name) : undefined}
                            disabled={prop.readOnly || false}
                            required={prop.required}
                            error={errorText}
                            rows={4}
                            maxLength={maxLength}
                            className="w-full"
                        />
                        {maxLength !== undefined && (
                            <span
                                className="text-xs text-text-secondary mt-1 block text-right"
                                aria-label={`${currentLength} z ${maxLength} znaků`}
                            >
                                {currentLength} / {maxLength}
                            </span>
                        )}
                    </div>
                );
            }}
        </Field>
    )
}

HalFormsTextArea.displayName = 'HalFormsTextArea'
