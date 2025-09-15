export interface PaginatedResponse<T> {
    data: {
        content: T[];
        page: {
            totalElements: number;
            totalPages: number;
            size: number;
            number: number;
        };
        _actions?: string[];
    };
}

export interface ApiParams {
    page: number;
    size: number;
    sort: string[];

    [key: string]: any;
}

export type SortDirection = 'asc' | 'desc';

export interface TableColumn {
    column: string;
    children: React.ReactNode;
}

export interface SortableTableColumn extends TableColumn {
    sortable: true;
}
