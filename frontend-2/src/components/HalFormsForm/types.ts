import type {HalFormsProperty} from "../../api";
import type {ReactElement} from "react";

interface HalFormsInputProps<T> {
    prop: HalFormsProperty,
    errorText?: string,
    value: T,
    onValueChanged: (attrName: string, value: T) => void
}

type HalFormFieldFactory = (fieldType: string, conf: HalFormsInputProps<any>) => ReactElement | null;


export {type HalFormsInputProps, type HalFormFieldFactory};