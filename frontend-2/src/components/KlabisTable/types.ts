import React from "react";

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

export interface ApiParams {
    page: number;
    size: number;
    sort: string[];

    [key: string]: any;
}

export type SortDirection = 'asc' | 'desc';

export interface TableCellRenderProps {
    item: any;
    column: string;
    value: any;
}

export interface TableCellProps {
    column: string;
    hidden?: boolean,
    sortable?: boolean,
    children: React.ReactNode;
    dataRender?: (props: TableCellRenderProps) => React.ReactNode;
}