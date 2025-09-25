import React, {createContext, isValidElement, type ReactElement, ReactNode, useContext, useState} from 'react';
import {type ApiParams, type SortDirection} from './types';

interface KlabisTableContextType {
    // Pagination state
    page: number;
    rowsPerPage: number;
    setPage: (page: number) => void;
    setRowsPerPage: (rowsPerPage: number) => void;

    // Sorting state
    orderBy?: string;
    orderDirection: SortDirection;
    setOrderBy: (orderBy: string, direction: SortDirection) => void;

    // Combined API params
    createApiParams: () => ApiParams;

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

class ColumnModel {
    name: string;
    headerCell: ReactElement;
    hidden: boolean;
    sortable: boolean;
    dataRender: (props: RenderProps) => ReactNode | undefined;

    constructor(name: string, headerCell: ReactElement, hidden: boolean, sortable: boolean, dataRender: (props: RenderProps) => ReactNode | undefined) {
        this.name = name;
        this.headerCell = headerCell;
        this.hidden = hidden;
        this.sortable = sortable;
        this.dataRender = dataRender;
    }
}

class TableModel<T> {
    columns: ColumnModel[];
    rows: T[];

    constructor(columns: ColumnModel[]) {
        this.columns = columns;
        this.rows = [];
    }
}

interface RenderProps {
    item: any;
    column: string;
    value: any;
}

function isReactComponent(item: ReactNode): item is ReactElement {
    return (item as ReactElement).props !== undefined;
}


const convertToTableColumn = (child: ReactNode): ColumnModel | null => {
    if (isReactComponent(child)) {
        if (!isValidElement(child)) {
            return false;
        }

        const hidden = child.props?.hidden as boolean;
        const column = child.props?.column as string;
        const renderFunc = child.props?.dataRender as ((props: RenderProps) => React.ReactNode) | undefined;

        return new ColumnModel(column, child, hidden, false, renderFunc);
    }

    return null;
}

const createModelFromChildren = <T, >(children): TableModel<T> => {
    const columns = React.Children.map(children, child => convertToTableColumn(child));
    return new TableModel<T>(columns);
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

    console.log(tableModel);

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
    function createApiParams(): ApiParams {
        return {
            page: page,
            size: rowsPerPage,
            sort: orderBy ? [`${orderBy},${orderDirection}`] : [],
            ...additionalParams
        } as ApiParams;
    }

    const contextValue: KlabisTableContextType = {
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
