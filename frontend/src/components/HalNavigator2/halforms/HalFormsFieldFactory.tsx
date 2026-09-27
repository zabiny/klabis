import {
    HalFormsCheckbox,
    HalFormsCheckboxGroup,
    HalFormsCollectionField,
    HalFormsDateTime,
    HalFormsInput,
    HalFormsMultiSelect,
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
    const multi = isMultipleProperty(conf.prop)

    if (multi && !conf.prop.suggest) {
        // Probe for a per-item widget with multi/multiple forced false (design.md D7), so the
        // answer is always "does a widget exist for one item", never "what would it render for
        // the raw multi value" — the only way to get MemberId right without misreading a
        // self-guarding custom type (e.g. PaymentRuleRequest) as "no widget".
        // The probe result is discarded: customFactory implementations must stay
        // side-effect-free and defer any real work into the component they return.
        const singleConf: HalFormsInputProps = {...conf, prop: {...conf.prop, multi: false, multiple: false}}
        const hasCustomWidget = customFactory?.(fieldType, singleConf) != null

        if (hasCustomWidget) {
            // Render one row per item through that same custom widget, sharing the same
            // options link across rows.
            return <HalFormsCollectionField {...conf} fieldFactory={boundFactory(customFactory)} />
        }

        if (conf.prop.options) {
            if (conf.prop.options.inline !== undefined && conf.prop.options.inline.length === 0) {
                return null
            }
            // Generic multi-value widget for any option-backed field without its own custom
            // widget (e.g. event type disciplineIds) — searchable dropdown + removable chips,
            // working with both inline and remote-link options via useHalFormOptions.
            return <HalFormsMultiSelect {...conf} />
        }

        // No options and no custom widget — a plain array of composite/basic values, e.g.
        // categories, payment rules.
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
