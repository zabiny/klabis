import React, {createContext, isValidElement, type ReactElement, type ReactNode, useContext, useState} from 'react';
import {Paging, type TableCellProps, type TableCellRenderProps} from './types';
import {TableCell as MuiTableCell} from "@mui/material";
import {type PaginatedApiParams, type SortDirection} from '../../api'

interface KlabisTableContextType {
    // Pagination state
    page: number;
    columnsCount: number;
    tableModel: TableModel;
    rowsPerPage: number;
    setPage: (page: number) => void;
    setRowsPerPage: (rowsPerPage: number) => void;

    // Sorting state
    orderBy?: string;
    orderDirection: SortDirection;
    setOrderBy: (orderBy: string, direction: SortDirection) => void;

    // Combined API params
    createApiParams: () => PaginatedApiParams;

    // Actions
    handleRequestSort: (column: string) => void;
    handleChangePage: (event: unknown, newPage: number) => void;
    handleChangeRowsPerPage: (event: React.ChangeEvent<HTMLInputElement>) => void;
}

const KlabisTableContext = createContext<KlabisTableContextType | undefined>(undefined);

interface KlabisTableProviderProps {
    children: ReactNode;
    colDefs: ReactNode;
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    defaultRowsPerPage?: number;
}

const defaultRenderFunc = (props: TableCellRenderProps): ReactNode => {
    return <span>{'' + props.value}</span>;
}

class ColumnModel {
    name: string;
    headerCell: ReactElement;
    hidden: boolean;
    sortable: boolean;
    key?: string;
    dataRender: (props: TableCellRenderProps) => ReactNode;

    constructor(name: string, headerCell: ReactElement, hidden: boolean, sortable: boolean, key: string | undefined, dataRender?: (props: TableCellRenderProps) => ReactNode) {
        this.name = name;
        this.headerCell = headerCell;
        this.hidden = hidden;
        this.sortable = sortable;
        this.key = key;
        this.dataRender = dataRender || defaultRenderFunc;
    }

    renderCellForRow(item: Record<string, unknown>): ReactNode {
        const cellContent = this.dataRender({item, column: this.name, value: item[this.name]});
        return (
            <MuiTableCell key={this.key || this.name}>
                {cellContent}
            </MuiTableCell>
        );
    }
}

class TableModel {
    columns: ColumnModel[];

    constructor(columns: ColumnModel[]) {
        this.columns = columns;
    }

    /**
     * Iterate over columns that are not hidden.
     * @param callback Function to execute for each visible column.
     */
    mapVisibleColumns<T, >(callback: (col: ColumnModel) => T): T[] {
        return this.columns.filter(col => !col.hidden).map(callback);
    }

    renderCellsForRow(row: Record<string, unknown>): ReactNode[] {
        return this.mapVisibleColumns(col => col.renderCellForRow(row));
    }

    renderHeaders() {
        return this.mapVisibleColumns(col => col.headerCell);
    }

}

function isTableCellComponent(item: ReactNode): item is ReactElement<TableCellProps> {
    return isValidElement(item) && (item.props as Partial<TableCellProps>).column !== undefined;
}


const convertToColumnModel = (child: ReactNode): ColumnModel | null => {
    if (isTableCellComponent(child)) {
        return new ColumnModel(child.props.column, child, child.props.hidden || false, child.props.sortable || false, child.key || undefined, child.props.dataRender || defaultRenderFunc);
    }

    return null;
}

const createModelFromChildren = (children: React.ReactNode): TableModel => {
    const columns = React.Children.map(children, child => convertToColumnModel(child));
    if (!columns) {
        return new TableModel([]);
    }
    return new TableModel(columns);
}


export const KlabisTableProvider: React.FC<KlabisTableProviderProps> = ({
                                                                            children,
                                                                            colDefs,
                                                                            defaultOrderBy,
                                                                            defaultOrderDirection = 'asc',
                                                                            defaultRowsPerPage = 10,
                                                                        }) => {
    const [page, setPage] = useState<Paging>({page: 0, rowsPerPage: defaultRowsPerPage});
    const [orderBy, setOrderBy] = useState<string | undefined>(defaultOrderBy);
    const [orderDirection, setOrderDirection] = useState<SortDirection>(defaultOrderDirection);

    const tableModel = createModelFromChildren(colDefs);

    const handleRequestSort = (column: string) => {
        const isAsc = orderBy === column && orderDirection === 'asc';
        setOrderDirection(isAsc ? 'desc' : 'asc');
        setOrderBy(column);
        setPage(prev => ({...prev, page: 0})); // Reset to first page when sorting changes
    };

    const handleChangePage = (_event: unknown, newPage: number) => {
        setPage(prev => ({...prev, page: newPage}));
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setPage({page: 0, rowsPerPage: parseInt(event.target.value, 10)})
    };

    // Vytvoření parametrů pro API volání
    function createApiParams(): PaginatedApiParams {
        return {
            page: page.page,
            size: page.rowsPerPage,
            sort: orderBy ? [`${orderBy},${orderDirection}`] : [],
        } as PaginatedApiParams;
    }

    const columnsCount = tableModel.columns.length;

    const contextValue: KlabisTableContextType = {
        columnsCount,
        tableModel,
        page: page.page,
        rowsPerPage: page.rowsPerPage,
        setPage: (p) => setPage((prev) => ({...prev, page: p})),
        setRowsPerPage: (rowsPerPage) => setPage((prev) => ({...prev, rowsPerPage})),
        orderBy,
        orderDirection,
        setOrderBy,
        createApiParams,
        handleRequestSort,
        handleChangePage,
        handleChangeRowsPerPage,
    };

    return (
        <KlabisTableContext.Provider value={contextValue}>
            {children}
        </KlabisTableContext.Provider>
    );
};

export const useKlabisTableContext = (): KlabisTableContextType => {
    const context = useContext(KlabisTableContext);
    if (context === undefined) {
        throw new Error('useKlabisTableContext must be used within a KlabisTableProvider');
    }
    return context;
};

export const useTableModel = (): TableModel => {
    const context = useContext(KlabisTableContext);
    if (context === undefined) {
        throw new Error('useTableModel must be used within a KlabisTableProvider');
    }
    return context.tableModel;
}