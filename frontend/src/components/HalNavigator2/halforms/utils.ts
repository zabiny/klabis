import {type HalFormsProperty, type HalFormsResponse, type HalFormsTemplate, type HalResponse, type Link} from "../../../api";

// Backend sends "multi" instead of "multiple" — normalizes both to a single boolean
export const isMultipleProperty = (prop: HalFormsProperty): boolean => !!(prop.multiple || prop.multi);

export function sanitizeFormValues(values: Record<string, unknown>): Record<string, unknown> {
    return Object.fromEntries(
        Object.entries(values).map(([key, value]) => [key, value === '' ? null : value])
    );
}

export const isHalFormsTemplate = (item: unknown): item is HalFormsTemplate => {
    return typeof item === 'object' && item !== null && 'properties' in item;
}

export const getSelfLink = (item: HalResponse): Link | undefined => {
    const self = item._links?.self as Link | Link[] | undefined;
    if (Array.isArray(self)) {
        return self[0];
    }
    return self;
}

export const getDefaultTemplate = (item: HalFormsResponse): HalFormsTemplate => {
    return Object.values(item._templates)[0];
}

export const isHalFormsResponse = (item: unknown): item is HalFormsResponse => {
    // HalForms response is HAL response with at least one template
    return isHalResponse(item) && !!item._templates && Object.values(item._templates).length > 0 && isHalFormsTemplate(Object.values(item._templates)[0]);
}

export const isKlabisFormResponse = (item: unknown): item is HalFormsResponse => {
    return isHalFormsResponse(item) && item._embedded === undefined;    // Klabis Forms response is only for single item (= there is no _embedded from CollectionModel)
}

export const isHalResponse = (item: unknown): item is HalResponse => {
    return typeof item === 'object' && item !== null && ('_links' in item || '_embedded' in item);
}