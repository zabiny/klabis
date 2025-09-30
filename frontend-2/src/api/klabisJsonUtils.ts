import {type KlabisHateoasObject, type PaginatedResponse} from "./types";

function isKlabisHateoasObject(item: KlabisHateoasObject | string[]) {
    return (item as KlabisHateoasObject)._actions !== undefined;
}

export const hasAction = (item: KlabisHateoasObject | string[] | undefined, action: string): boolean => {
    if (item === undefined) {
        return false;
    }

    if (Array.isArray(item)) {
        return item.includes(action) || false;
    }

    if (isKlabisHateoasObject(item)) {
        return hasAction(item._actions || [], action);
    }

    return false;
}

export const isPaginatedResponse = (item: any): item is PaginatedResponse<object> => {
    if (item === undefined) {
        return true;
    }
    return (item as PaginatedResponse<object>).content !== undefined && (item as PaginatedResponse<object>).page !== undefined;
}


