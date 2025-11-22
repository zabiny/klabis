import type {components} from "./klabisApi";

export type Link = components["schemas"]["Link"];

export interface PaginatedResponse<T> {
    content: T[];
    page: {
        totalElements: number;
        totalPages: number;
        size: number;
        number: number;
    };
    _actions?: string[];
}

export interface PaginatedApiParams {
    page: number;
    size: number;
    sort: string[];

    [key: string]: any;
}

export type SortDirection = 'asc' | 'desc';

export type KlabisActionName = string;
export type KlabisAction = KlabisActionName | Link;
export type KlabisActions = Array<KlabisActionName> | Record<string, Link>;

export interface KlabisHateoasObject {
    _actions?: KlabisActions
}

export interface HalResponse {
    _links?: {
        [rel: string]: Link | Array<Link>;
    }
    _embedded?: object,

    // allow arbitrary additional properties
    [key: string]: any;
}

export interface HalFormsResponse extends HalResponse {
    _templates: {
        [name: string]: HalFormsTemplate;
    };
}

// --- Typy ---

export type HalFormsOptionValue = string | number;

export type HalFormsOptionType = OptionItem | HalFormsOptionValue;

/// http://rwcbook.com/hal-forms/#options-element
export interface HalFormsOption {
    inline?: HalFormsOptionType[]
    link?: Link
}

export interface OptionItem {
    value: HalFormsOptionValue;
    prompt?: string;
}

export interface HalFormsProperty {
    name: string;
    prompt?: string;
    type: string; // "text" | "number" | "email" | "textarea" | "radioGroup" | "checkboxGroup"
    value?: string | number;
    required?: boolean;
    regex?: string;
    readOnly?: boolean;
    options?: HalFormsOption;
    multiple?: boolean;
}

// only update methods as template represents "request body"
export type HalFormsTemplateMethod = "POST" | "PUT" | "DELETE";

export interface TemplateTarget {
    target: string,
    method?: HalFormsTemplateMethod
}

export interface HalFormsTemplate {
    method?: HalFormsTemplateMethod
    target?: string,
    contentType?: string;
    title?: string;
    properties: Array<HalFormsProperty>
}


