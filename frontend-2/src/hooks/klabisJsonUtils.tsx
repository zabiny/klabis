interface KlabisHateoasObject {
    _actions: Array<string>
}

export const hasAction = (item: KlabisHateoasObject, action: string): boolean => {
    return Array.isArray(item?._actions || null) && item._actions.includes(action);
}

