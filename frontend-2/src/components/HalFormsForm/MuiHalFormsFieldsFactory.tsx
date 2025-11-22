import {
    HalFormsBoolean,
    HalFormsCheckbox,
    HalFormsCheckboxGroup,
    HalFormsInput,
    HalFormsRadio,
    HalFormsSelect,
    HalFormsTextArea
} from "./MuiHalFormsFields";
import {type HalFormFieldFactory, type HalFormsInputProps} from "./types";
import {type ReactElement} from "react";

export const muiHalFormsFieldsFactory = (fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
    switch (fieldType) {
        case 'checkboxGroup':
            return <HalFormsCheckboxGroup {...conf}/>;
        case 'checkbox':
            return <HalFormsCheckbox {...conf}/>;
        case 'radioGroup':
            return <HalFormsRadio {...conf}/>;
        case 'select':
            return <HalFormsSelect {...conf}/>;
        case 'boolean':
            return <HalFormsBoolean {...conf}/>;
        case 'textarea':
            return <HalFormsTextArea {...conf}/>;
        case "text":
        case "email":
        case "number":
        case"date":
            return <HalFormsInput {...conf}/>;
        default:
            return null;
    }
}

export const expandMuiFieldsFactory = (additionalFactory: HalFormFieldFactory): HalFormFieldFactory => {
    return (fieldType: string, conf: HalFormsInputProps): ReactElement | null => {
        const result = additionalFactory(fieldType, conf);
        if (result) {
            return result;
        }

        return muiHalFormsFieldsFactory(fieldType, conf);
    };
}