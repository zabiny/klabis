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
}

export interface HalFormsResponse extends HalResponse {
    _templates?: {
        [name: string]: HalFormsTemplate;
    };

    // allow arbitrary additional properties
    [key: string]: any;
}

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

// only update methods as template represents "request body"
export type HalFormsTemplateMethod = "POST" | "PUT" | "DELETE";

export interface HalFormsTemplate {
    method?: HalFormsTemplateMethod;
    contentType?: string;
    properties: Array<HalFormsProperty>
}