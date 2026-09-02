import {
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

export type CustomFieldFactory = (fieldType: string, conf: HalFormsInputProps) => ReactElement | null;

/**
 * boundFactory - binds a custom factory into a HalFormFieldFactory that also
 * knows the built-in types, for HalFormsCollectionField to recurse per-row.
 */
const boundFactory = (customFactory?: CustomFieldFactory): HalFormFieldFactory =>
    (fieldType, conf) => halFormsFieldsFactory(fieldType, conf, customFactory)

/**
 * halFormsFieldsFactory - Factory function for creating HAL+Forms field components
 * Maps HAL+Forms field types to custom FormFields-based components
 *
 * Replaces MUI-based factory with abstraction layer components
 */
export const halFormsFieldsFactory = (
    fieldType: string,
    conf: HalFormsInputProps,
    customFactory?: CustomFieldFactory
): ReactElement | null => {
    if (isMultipleProperty(conf.prop) && !conf.prop.options && !conf.prop.suggest) {
        return <HalFormsCollectionField {...conf} fieldFactory={boundFactory(customFactory)} />
    }

    const custom = customFactory?.(fieldType, conf)
    if (custom) {
        return custom
    }

    if (conf.prop.options?.inline !== undefined && conf.prop.options.inline.length === 0) {
        return null
    }

    if (conf.prop.options) {
        if (isMultipleProperty(conf.prop)) {
            return <HalFormsCheckboxGroup {...conf} />
        }
        return <HalFormsSelect {...conf} />
    }

    // The backend renders java.lang.Boolean HAL-FORMS properties with type "Boolean"
    // (Spring HATEOAS has no HtmlInputType for it); "boolean" covers the primitive.
    switch (fieldType) {
        case 'checkboxGroup':
            return <HalFormsCheckboxGroup {...conf} />
        case 'checkbox':
        case 'Boolean':
        case 'boolean':
            return <HalFormsCheckbox {...conf} />
        case 'radioGroup':
            return <HalFormsRadio {...conf} />
        case 'select':
            return <HalFormsSelect {...conf} />
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
 * expandHalFormsFieldFactory - Allows extending the field factory with custom field types.
 * Composes the custom logic via halFormsFieldsFactory's 3rd `customFactory` parameter
 * (see design.md D4), so custom types resolve inside collections through the same
 * full factory HalFormsCollectionField recurses with — no wrapper-induced recursion gap.
 */
export const expandHalFormsFieldFactory = (
    additionalFactory: CustomFieldFactory
): HalFormFieldFactory => boundFactory(additionalFactory)
