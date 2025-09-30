import React, {createContext, isValidElement, type ReactElement, type ReactNode, useContext, useState} from 'react';
import {type TableCellProps, type TableCellRenderProps} from './types';
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
    additionalParams?: Record<string, any>;
}

const defaultRenderFunc = (props: TableCellRenderProps): ReactNode => {
    return props.value;
}

class ColumnModel {
    name: string;
    headerCell: ReactElement;
    hidden: boolean;
    sortable: boolean;
    key?: string;
    dataRender: (props: TableCellRenderProps) => ReactNode;

    constructor(name: string, headerCell: ReactElement, hidden: boolean, sortable: boolean, key: string | undefined, dataRender: (props: TableCellRenderProps) => ReactNode) {
        this.name = name;
        this.headerCell = headerCell;
        this.hidden = hidden;
        this.sortable = sortable;
        this.key = key;
        this.dataRender = dataRender;
    }

    renderCellForRow(item: Record<string, any>): ReactNode {
        const value = this.getCellValue(item);
        const cellContent = this.dataRender({item, column: this.name, value});
        return (
            <MuiTableCell key={this.key || this.name}>
                {cellContent}
            </MuiTableCell>
        );
    }

    getCellValue(row: Record<string, any>): ReactNode {
        return row[this.name];
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

    renderCellsForRow(row: object): ReactNode[] {
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

const createModelFromChildren = (children): TableModel => {
    const columns = React.Children.map(children, child => convertToColumnModel(child));
    return new TableModel(columns);
}

export const KlabisTableProvider: React.FC<KlabisTableProviderProps> = ({
                                                                            children,
                                                                            colDefs,
                                                                            defaultOrderBy,
                                                                            defaultOrderDirection = 'asc',
                                                                            defaultRowsPerPage = 10,
                                                                            additionalParams = {},
                                                                        }) => {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage);
    const [orderBy, setOrderBy] = useState<string | undefined>(defaultOrderBy);
    const [orderDirection, setOrderDirection] = useState<SortDirection>(defaultOrderDirection);

    const tableModel = createModelFromChildren(colDefs);

    const handleRequestSort = (column: string) => {
        const isAsc = orderBy === column && orderDirection === 'asc';
        setOrderDirection(isAsc ? 'desc' : 'asc');
        setOrderBy(column);
        setPage(0); // Reset to first page when sorting changes
    };

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    // Vytvoření parametrů pro API volání
    function createApiParams(): PaginatedApiParams {
        return {
            page: page,
            size: rowsPerPage,
            sort: orderBy ? [`${orderBy},${orderDirection}`] : [],
            ...additionalParams
        } as PaginatedApiParams;
    }

    const columnsCount = tableModel.columns.length;

    const contextValue: KlabisTableContextType = {
        columnsCount,
        tableModel,
        page,
        rowsPerPage,
        setPage,
        setRowsPerPage,
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