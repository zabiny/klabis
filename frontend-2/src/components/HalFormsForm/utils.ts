import {type HalFormsResponse, type HalFormsTemplate, HalResponse} from "../../api";

export const isHalFormsTemplate = (item: any): item is HalFormsTemplate => {
    return item !== undefined && item !== null && item.properties !== undefined && item.method !== undefined;
}

export const isHalFormsResponse = (item: any): item is HalFormsResponse => {
    return item !== undefined && item !== null && item._templates !== undefined && item._links !== undefined && isHalFormsTemplate(item._templates?.default);
}

export const isKlabisFormResponse = (item: any): item is HalFormsResponse => {
    return isHalFormsResponse(item) && item._embedded === undefined;    // Klabis Forms response is only for single item (= there is no _embedded from CollectionModel)
}

export const isHalResponse = (item: any): item is HalResponse => {
    return item !== undefined && item !== null && (typeof item._links === "object" || item._embedded);
}