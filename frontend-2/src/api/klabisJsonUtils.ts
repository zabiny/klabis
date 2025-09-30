import {
    type HalModel,
    type KlabisAction,
    type KlabisActionName,
    type KlabisHateoasObject,
    type PaginatedResponse
} from "./types";
import type {KlabisApiGetPaths, Link} from "./index";

function isKlabisHateoasObject(item: KlabisHateoasObject | string[]) {
    return (item as KlabisHateoasObject)._actions !== undefined;
}

function isHalModel(item: any): item is HalModel {
    return (item as HalModel)._links !== undefined;
}

export const findAction = (rel: string, actions: KlabisAction[]): KlabisAction | undefined => {
    return actions.find(l => isLink(l) && l.rel === rel || rel === l);
}

export const hasAction = (item: KlabisHateoasObject | KlabisAction[] | undefined, action: KlabisActionName): boolean => {
    if (item === undefined) {
        return false;
    }

    if (Array.isArray(item)) {
        return findAction(action, item) !== undefined || false;
    }

    if (isKlabisHateoasObject(item)) {
        return hasAction(item._actions || [], action);
    }

    if (isHalModel(item)) {
        return hasAction(item._links || [], action);
    }

    return false;
}

export const isPaginatedResponse = (item: any): item is PaginatedResponse<object> => {
    if (item === undefined) {
        return true;
    }
    return (item as PaginatedResponse<object>).content !== undefined && (item as PaginatedResponse<object>).page !== undefined;
}


export const isLink = (item: any): item is Link => {
    return (item as Link).href !== undefined;
}

export const getApiPath = (url: Link | KlabisApiGetPaths): KlabisApiGetPaths => {
    if (isLink(url)) {
        return url.href?.substring("https://localhost:8443".length) as KlabisApiGetPaths;
    } else {
        return url;
    }
}

