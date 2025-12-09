import React from "react";
import {type PageMetadata} from "../../api";

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
    rowsPerPage: number,
    itemsCount: number
}

export interface TableData<T> {
    page: PageMetadata,
    data: T[]
}

export type FetchTableDataCallback<T> = (page: Omit<Paging, "itemsCount">) => Promise<TableData<T>>;