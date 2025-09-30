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
export type KlabisAction = KlabisActionName;
export type KlabisActions = Array<KlabisAction>;

export interface KlabisHateoasObject {
    _actions?: KlabisActions
}