export type KlabisActionName = string;

export type KlabisAction = KlabisActionName;

export type KlabisActions = Array<KlabisAction>;

export interface KlabisHateoasObject {
    _actions?: KlabisActions
}

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


