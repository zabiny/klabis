import type {HalFormsProperty} from "../../api";
import type {ReactElement} from "react";

interface SubElementConfiguration {
    prompt?: string,
    type?: string
}

interface HalFormsInputProps {
    prop: HalFormsProperty,
    errorText?: string,
    subElementProps: (attrName: string, configuration: SubElementConfiguration) => HalFormsInputProps
}

type HalFormFieldFactory = (fieldType: string, conf: HalFormsInputProps) => ReactElement | null;


export {type HalFormsInputProps, type HalFormFieldFactory, type SubElementConfiguration};