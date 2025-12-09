import React from "react";
import {PageMetadata} from "../../api";

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

export interface TableData {
    page: PageMetadata,
    data: Record<string, unknown>[]
}

export type FetchTableDataCallback = (page: Paging) => Promise<TableData>;