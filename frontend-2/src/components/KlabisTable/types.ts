import React from "react";
import {type PaginatedApiParams} from "../../api";

export interface TableCellRenderProps {
    item: Record<string, unknown>;
    column: string;
    value: unknown;
}

export interface TableCellProps {
    column: string;
    hidden?: boolean,
    sortable?: boolean,
    children: React.ReactNode;
    dataRender?: (props: TableCellRenderProps) => React.ReactNode;
}


export interface Paging {
    page: number,
    rowsPerPage: number
}

export interface TablePageData {
    size: number,
    totalElements: number,
    totalPages: number,
    number: number
}


export interface TableData<T> {
    page?: TablePageData,
    data: T[]
}

export type FetchTableDataCallback<T> = (apiParams: PaginatedApiParams) => Promise<TableData<T>>;