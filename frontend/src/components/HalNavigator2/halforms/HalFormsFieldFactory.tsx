import {
    HalFormsBoolean,
    HalFormsCheckbox,
    HalFormsCheckboxGroup,
    HalFormsCollectionField,
    HalFormsDateTime,
    HalFormsInput,
    HalFormsRadio,
    HalFormsSelect,
    HalFormsTextArea,
} from './fields'
import {type HalFormFieldFactory, type HalFormsInputProps} from './types.ts'
import {isMultipleProperty} from './utils.ts'
import {type ReactElement} from 'react'

/**
 * halFormsFieldsFactory - Factory function for creating HAL+Forms field components
 * Maps HAL+Forms field types to custom FormFields-based components
 *
 * Replaces MUI-based factory with abstraction layer components
 */
export const halFormsFieldsFactory = (
    fieldType: string,
    conf: HalFormsInputProps
): ReactElement | null => {
    if (isMultipleProperty(conf.prop) && !conf.prop.options && !conf.prop.suggest) {
        return <HalFormsCollectionField {...conf} />
    }

    if (conf.prop.options?.inline !== undefined && conf.prop.options.inline.length === 0) {
        return null
    }

    if (conf.prop.options) {
        return <HalFormsSelect {...conf} />
    }

    switch (fieldType) {
        case 'checkboxGroup':
            return <HalFormsCheckboxGroup {...conf} />
        case 'checkbox':
            return <HalFormsCheckbox {...conf} />
        case 'radioGroup':
            return <HalFormsRadio {...conf} />
        case 'select':
            return <HalFormsSelect {...conf} />
        case 'boolean':
            return <HalFormsBoolean {...conf} />
        case 'textarea':
            return <HalFormsTextArea {...conf} />
        case 'datetime':
            return <HalFormsDateTime {...conf} />
        case 'text':
        case 'email':
        case 'number':
        case 'date':
        case 'url':
        case 'tel':
            return <HalFormsInput {...conf} />
        default:
            return null
    }
}

/**
 * expandHalFormsFieldFactory - Allows extending the field factory with custom field types
 * Returns a new factory that tries the additional factory first, then falls back to the default
 */
export const expandHalFormsFieldFactory = (
    additionalFactory: HalFormFieldFactory
): HalFormFieldFactory => {
    return (fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
        const result = additionalFactory(fieldType, conf)
        if (result) {
            return result
        }

        return halFormsFieldsFactory(fieldType, conf)
    }
}
