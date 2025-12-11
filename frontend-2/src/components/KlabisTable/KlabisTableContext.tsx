import React, {
    createContext,
    isValidElement,
    type ReactElement,
    type ReactNode,
    useContext,
    useEffect,
    useState
} from 'react';
import type {
    FetchTableDataCallback,
    Paging,
    TableCellProps,
    TableCellRenderProps,
    TableData,
    TablePageData
} from './types';
import {TableCell as MuiTableCell} from "@mui/material";
import {type PaginatedApiParams, type SortDirection} from '../../api'

interface KlabisTableContextType<T> {
    rows: T[],
    tableModel: TableModel;

    // Pagination state
    paging?: TablePageData,
    // Sorting state
    sort?: Sort,

    // Actions
    handleRequestSort: (column: string) => void;
    handlePagingChange: (page: number, rowsPerPage: number) => void;
}

const KlabisTableContext = createContext<KlabisTableContextType<unknown> | undefined>(undefined);

interface KlabisTableProviderProps<T> {
    children: ReactNode;
    colDefs: ReactNode;
    fetchData: FetchTableDataCallback<T>;
    initialData?: TableData<T>,
    defaultSort?: Sort,
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

interface Sort {
    by: string,
    direction: SortDirection
}

function reverseDirection(sort: Sort): Sort {
    return {...sort, direction: sort.direction === 'asc' ? 'desc' : 'asc'}
}

export const KlabisTableProvider: React.FC<KlabisTableProviderProps<unknown>> = ({
                                                                                     children,
                                                                                     colDefs,
                                                                                     fetchData,
                                                                                     initialData,
                                                                                     defaultSort,
                                                                                     defaultRowsPerPage = 10,
                                                                                 }) => {
    const [page, setPage] = useState<Paging>({page: 0, rowsPerPage: defaultRowsPerPage});
    const [sort, setSort] = useState<Sort | undefined>(defaultSort)
    const [tableRows, setTableRows] = useState<TableData<unknown> | undefined>(initialData);

    const tableModel = createModelFromChildren(colDefs);

    const handleRequestSort = (column: string) => {
        setSort(prev => {
            if (!prev || prev.by !== column) {
                return {by: column, direction: 'asc'};
            } else {
                return reverseDirection(prev);
            }
        })
        setPage(prev => ({...prev, page: 0})); // Reset to first page when sorting changes
    };

    const handlePagingChange = (newPage: number, newRowsPerPage: number) => setPage(prev => ({
        ...prev,
        page: newPage,
        rowsPerPage: newRowsPerPage
    }))

    // Fetch data asynchronously via provided callback whenever paging changes
    useEffect(() => {
        if (initialData) {
            return;
        }
        let cancelled = false;
        (async () => {
            try {
                const apiPagingParams: PaginatedApiParams = {
                    page: page.page,
                    size: page.rowsPerPage,
                    sort: sort ? [`${sort.by},${sort.direction}`] : [],
                }
                const result = await fetchData(apiPagingParams);
                if (!cancelled) setTableRows(result);
            } catch (e) {
                if (!cancelled) setTableRows(undefined);
                console.log(e)
            }
        })();
        return () => {
            cancelled = true;
        };
    }, [fetchData, page, sort, initialData]);

    const contextValue: KlabisTableContextType<unknown> = {
        tableModel,
        rows: tableRows?.data || [],
        paging: tableRows?.page ? {...tableRows.page, size: page.rowsPerPage} : undefined,
        sort: sort,
        handleRequestSort,
        handlePagingChange
    };

    return (
        <KlabisTableContext.Provider value={contextValue}>
            {children}
        </KlabisTableContext.Provider>
    );
};

export const useKlabisTableContext = <T extends Record<string, unknown>>(): KlabisTableContextType<T> => {
    const context = useContext(KlabisTableContext);
    if (context === undefined) {
        throw new Error('useKlabisTableContext must be used within a KlabisTableProvider');
    }
    return context as KlabisTableContextType<T>;
};