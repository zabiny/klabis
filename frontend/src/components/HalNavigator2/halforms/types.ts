import type {HalFormsProperty} from "../../../api";
import type {ReactElement} from "react";

interface SubElementConfiguration {
    prompt?: string,
    type?: string
}

type RenderMode = 'field' | 'input';

interface HalFormsInputProps {
    prop: HalFormsProperty,
    errorText?: string,
    renderMode?: RenderMode,
    subElementProps: (attrName: string, configuration?: SubElementConfiguration) => HalFormsInputProps,
    fieldFactory?: HalFormFieldFactory,
}

type HalFormFieldFactory = (fieldType: string, conf: HalFormsInputProps) => ReactElement | null;


const SIMPLE_FIELD_TYPES = new Set([
    'text', 'email', 'number', 'date', 'url', 'tel',
    'textarea', 'select', 'radioGroup', 'checkbox', 'checkboxGroup',
    'boolean', 'datetime'
]);

export {type HalFormsInputProps, type HalFormFieldFactory, type SubElementConfiguration, type RenderMode, SIMPLE_FIELD_TYPES};