import {type HalFormsTemplate} from "../../api";

export interface HalFormsFormProps {
    data: Record<string, any>;
    template: HalFormsTemplate;
    onSubmit?: (values: Record<string, any>) => void;
    submitButtonLabel?: string
}

export {HalFormsForm} from './HalFormsForm';