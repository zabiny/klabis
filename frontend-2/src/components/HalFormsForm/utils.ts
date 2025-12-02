import {type HalFormsResponse, type HalFormsTemplate, type HalResponse, type Link} from "../../api";

export const isHalFormsTemplate = (item: any): item is HalFormsTemplate => {
    return item !== undefined && item !== null && item.properties !== undefined && item.method !== undefined;
}

export const getSelfLink = (item: HalResponse): Link => {
    return item._links?.self;
}

export const getDefaultTemplate = (item: HalFormsResponse): HalFormsTemplate => {
    return Object.values(item._templates)[0];
}

export const isHalFormsResponse = (item: any): item is HalFormsResponse => {
    // HalForms response is HAL response with at least one template
    return isHalResponse(item) && item._templates && Object.values(item._templates).length > 0 && isHalFormsTemplate(getDefaultTemplate(item));
}

export const isKlabisFormResponse = (item: any): item is HalFormsResponse => {
    return isHalFormsResponse(item) && item._embedded === undefined;    // Klabis Forms response is only for single item (= there is no _embedded from CollectionModel)
}

export const isHalResponse = (item: any): item is HalResponse => {
    return item !== undefined && item !== null && (item._links !== undefined || item._embedded !== undefined);
}