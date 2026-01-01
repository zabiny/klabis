import React from "react";
import {type SortDirection} from "../../api";

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

// Sort state representation
export interface SortState {
    by: string;
    direction: SortDirection;
}

// Pure UI table component props
// This is the pure presentation component with no data fetching
export interface KlabisTableProps<T extends Record<string, unknown>> {
    // Data (required - from parent)
    data: T[];
    page?: TablePageData;

    // Error state
    error?: Error | null;

    // User interaction callbacks
    onSortChange?: (column: string, direction: SortDirection) => void;
    onPageChange?: (page: number) => void;
    onRowsPerPageChange?: (rowsPerPage: number) => void;
    onRowClick?: (item: T) => void;

    // UI configuration
    children: React.ReactNode;
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    emptyMessage?: string;
    rowsPerPageOptions?: number[];

    // Controlled state (from parent)
    rowsPerPage?: number;
    currentPage?: number;
    currentSort?: SortState;
}
