import {Link} from "./index";

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

export interface HalModel {
    _links?: Link[]
}