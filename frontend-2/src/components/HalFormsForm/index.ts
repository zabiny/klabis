// --- Typy ---
export interface HalFormsOption {
    value?: string | number;
    prompt?: string;
}

export interface HalFormsProperty {
    name: string;
    prompt?: string;
    type?: string; // "text" | "number" | "email" | "textarea" | "radio"
    value?: string | number;
    required?: boolean;
    options?: (HalFormsOption | string | number)[];
    multiple?: boolean;
}

export interface HalFormsTemplate {
    properties: HalFormsProperty[];
}

export interface HalFormsFormProps {
    data: Record<string, any>;
    template: HalFormsTemplate;
    onSubmit?: (values: Record<string, any>) => void;
}

export interface HalFormsResponse {
    content: any,
    _links: {
        [rel: string]: {
            href: string;
            templated?: boolean;
        } | Array<{
            href: string;
            templated?: boolean;
        }>;
    };
    _templates?: {
        [name: string]: {
            method?: string;
            contentType?: string;
            properties: {
                [propName: string]: {
                    type?: string;
                    title?: string;
                    default?: any;
                    required?: boolean;
                    readOnly?: boolean;
                    enum?: any[];
                    minLength?: number;
                    maxLength?: number;
                    pattern?: string;
                };
            };
        };
    };

    // allow arbitrary additional properties
    [key: string]: any;
}

export {HalFormsForm} from './HalFormsForm';