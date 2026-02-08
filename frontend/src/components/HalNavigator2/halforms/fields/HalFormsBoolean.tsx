import type {ReactElement} from 'react'
import {useField} from 'formik'
import {SwitchField} from '../../../UI/forms'
import type {HalFormsInputProps} from '../types.ts'

/**
 * HalFormsBoolean component - switch/toggle for HAL+Forms boolean values
 * Uses Formik useField hook and FormFields SwitchField abstraction
 */
export const HalFormsBoolean = ({prop, errorText}: HalFormsInputProps): ReactElement => {
    const [field, , helpers] = useField(prop.name);

    return (
        <SwitchField
            name={prop.name}
            label={prop.prompt || prop.name}
            required={prop.required}
            disabled={prop.readOnly || false}
            error={errorText}
            checked={Boolean(field.value)}
            onChange={(checked: boolean) => helpers.setValue(checked)}
        />
    )
}

HalFormsBoolean.displayName = 'HalFormsBoolean'
