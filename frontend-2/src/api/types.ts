import type {components} from "./klabisApi";
import {isLink} from "./klabisJsonUtils";
import {isString} from "formik";

export type Link = components["schemas"]["Link"];
export type PageMetadata = components["schemas"]["PageMetadata"];

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

// Specific embedded resource type - allows typed access to known embedded structures
export interface HalEmbeddedResources {
    [key: string]: unknown | unknown[];
}

export interface HalResponse {
    _links?: {
        [rel: string]: Link | Array<Link>;
    }
    _embedded?: HalEmbeddedResources;
    _templates?: {
        [name: string]: HalFormsTemplate;
    };

    // allow arbitrary additional properties
    [key: string]: unknown;
}

export interface HalCollectionResponse extends HalResponse {
    page: PageMetadata
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

export function isTemplateTarget(item: any): item is TemplateTarget {
    return item && item.target;
}

export function isFormTarget(item: any): item is TemplateTarget {
    return isTemplateTarget(item) && !!item.method && ['POST', 'PUT', 'DELETE', 'PATCH'].indexOf(item.method) !== -1;
}

export type NavigationTarget = Link | TemplateTarget | string;

export function isNavigationTarget(item: unknown): item is NavigationTarget {
    return isLink(item) || isTemplateTarget(item) || isString(item);
}

export type EntityModel<T> = T & { _links: { [rel: string]: Link | Link[] } };

export type PagedModel<T> = { content: EntityModel<T>[], _links: { [rel: string]: Link | Link[] }, page: PageMetadata }

// Type guards for HAL responses with specific structures

// Check if response has templates
export function isHalResponseWithTemplates(item: unknown): item is HalResponse & {
    _templates: Record<string, HalFormsTemplate>
} {
    return typeof item === 'object' && item !== null && '_templates' in item &&
        typeof (item as any)._templates === 'object';
}

// Calendar item specific embedded resource type
export interface CalendarItemEmbedded extends HalEmbeddedResources {
    calendarItems?: Array<{
        start: string;
        end: string;
        note: string;
        _links: {
            event?: { href: string };
            self: { href: string };
        };
    }>;
}

// Type guard for calendar items structure (fixes CalendarPage error)
export function hasCalendarItems(data: unknown): data is HalResponse & { _embedded: CalendarItemEmbedded } {
    return typeof data === 'object' && data !== null &&
        '_embedded' in data && typeof (data as any)._embedded === 'object' &&
        'calendarItems' in (data as any)._embedded;
}
