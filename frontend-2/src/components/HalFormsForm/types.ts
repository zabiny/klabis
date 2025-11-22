import type {HalFormsProperty} from "../../api";
import type {ReactElement} from "react";

interface SubElementConfiguration {
    prompt?: string,
    type?: string
}

interface HalFormsInputProps<T> {
    prop: HalFormsProperty,
    errorText?: string,
    value: T,
    onValueChanged: (attrName: string, value: T) => void,
    subElementProps: (attrName: string, configuration: SubElementConfiguration) => HalFormsInputProps<any>
}

type HalFormFieldFactory = (fieldType: string, conf: HalFormsInputProps<any>) => ReactElement | null;


export {type HalFormsInputProps, type HalFormFieldFactory, type SubElementConfiguration};