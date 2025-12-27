import React from "react";
import {type PaginatedApiParams, type SortDirection} from "../../api";

// Cell rendering props
export interface TableCellRenderProps {
    item: Record<string, unknown>;
    column: string;
    value: unknown;
}

// Column definition component props
export interface TableCellProps {
    column: string;
    hidden?: boolean;
    sortable?: boolean;
    children: React.ReactNode;
    dataRender?: (props: TableCellRenderProps) => React.ReactNode;
}

// Pagination data from API
export interface TablePageData {
    size: number;
    totalElements: number;
    totalPages: number;
    number: number;
}

// Data returned from API or provided statically
export interface TableData<T> {
    page?: TablePageData;
    data: T[];
}

// Callback for fetching data
export type FetchTableDataCallback<T> = (apiParams: PaginatedApiParams) => Promise<TableData<T>>;

// Table state representation
export interface TableState {
    page: number;
    rowsPerPage: number;
    sort?: {
        by: string;
        direction: SortDirection;
    };
}

// Main component props - supports both static data and auto-fetching
export interface KlabisTableProps<T extends Record<string, unknown>> {
    // Data source (choose one):
    // - Provide data/page for static data (parent manages state)
    // - Provide fetchData for auto-fetching (component manages state)
    data?: T[];
    page?: TablePageData;
    fetchData?: FetchTableDataCallback<T>;

    // State callback (required for parent to track state changes)
    onStateChange?: (state: TableState) => void;

    // UI props
    children: React.ReactNode;
    onRowClick?: (item: T) => void;
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    emptyMessage?: string;
    rowsPerPageOptions?: number[];
    defaultRowsPerPage?: number;
}
