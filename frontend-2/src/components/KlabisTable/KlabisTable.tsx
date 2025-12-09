import React, {type ReactElement, type ReactNode, useEffect, useState} from 'react';
// Import pro MuiTableCell
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow,} from '@mui/material';
import {type SortDirection} from '../../api';
import {KlabisTableProvider, useKlabisTableContext} from "./KlabisTableContext.tsx";
import {type FetchTableDataCallback, type Paging, type TableData} from './types';

const KlabisTablePagination = ({itemsCount}: { itemsCount: number }): ReactElement => {
    const {paging, handlePagingChange: onPagingChange} = useKlabisTableContext();
    return paging && <TablePagination
        rowsPerPageOptions={[5, 10, 25, 50]}
        component="div"
        count={itemsCount}
        rowsPerPage={paging.rowsPerPage}
        page={paging.page}
        onPageChange={(_event: unknown, pageNumber: number) => onPagingChange(pageNumber, paging.rowsPerPage)}
        onRowsPerPageChange={(event) => onPagingChange(paging.page, parseInt(event.target.value, 10))}
        labelRowsPerPage="Řádků na stránku:"
        labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
    />;
}

interface KlabisTableProps<T extends Record<string, unknown>> {
    fetchData: FetchTableDataCallback<T>;
    children: React.ReactNode;
    onRowClick?: (item: T) => void;
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    defaultRowsPerPage?: number;
    emptyMessage?: string;
}

interface KLabisUiProps<T extends Record<string, unknown>> {
    rows: T[],
    paging?: Paging,
    onRowClick?: (item: T) => void,
    totalRowsCount?: number,
    emptyMessage?: string
}

export const KlabisTableInner = <T extends Record<string, unknown>>({
                                                                        rows,
                                                                        totalRowsCount,
                                                                        onRowClick,
                                                                        emptyMessage = "Žádná data",
                                                                    }: KLabisUiProps<T>) => {
    return (
        <Paper>
            <TableContainer>
                <Table>
                    <KlabisTableHeaders/>
                    <KlabisTableBody rows={rows} onRowClick={onRowClick} emptyMessage={emptyMessage}/>
                </Table>
            </TableContainer>
            {totalRowsCount && <KlabisTablePagination itemsCount={totalRowsCount}/>}
        </Paper>
    );
};

const KlabisTableBody = <T extends Record<string, unknown>>({rows, onRowClick, emptyMessage = "Žádná data"}: {
    rows: T[],
    onRowClick?: (item: T) => void,
    emptyMessage?: string
}): ReactNode => {
    const {tableModel} = useKlabisTableContext();

    const renderRows = (rows?: T[]): ReactNode => {
        if (!rows || rows.length == 0) {
            return (
                <TableRow key={0}><TableCell align={"center"}
                                             colSpan={tableModel.columns.length}>{emptyMessage}</TableCell></TableRow>
            );
        }

        return rows
            .map((item: T, index: number) => (
                <TableRow
                    key={'' + item?.id || index}
                    hover
                    onClick={() => onRowClick && onRowClick(item)}
                    sx={{cursor: onRowClick ? 'pointer' : 'default'}}
                >
                    {tableModel.renderCellsForRow(item)}
                </TableRow>
            ));
    };

    return (<TableBody>
        {renderRows(rows)}
    </TableBody>);

}

const KlabisTableHeaders = (): ReactNode => {
    const {tableModel} = useKlabisTableContext();

    return <TableHead>
        <TableRow>
            {tableModel.renderHeaders()}
        </TableRow>
    </TableHead>;
}

export const KlabisTable = <T extends Record<string, unknown>>(props: KlabisTableProps<T>) => {
    const {
        children,
        defaultOrderBy,
        defaultOrderDirection,
        defaultRowsPerPage = 10,
        fetchData,
        onRowClick,
        emptyMessage
    } = props;

    const [data, setData] = useState<TableData<T> | undefined>(undefined);

    // Fetch data asynchronously via provided callback whenever paging changes
    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                const result = await fetchData({page: 0, rowsPerPage: defaultRowsPerPage});
                if (!cancelled) setData(result);
            } catch (e) {
                if (!cancelled) setData(undefined);
            }
        })();
        return () => {
            cancelled = true;
        };
    }, [fetchData]);

    return <KlabisTableProvider
        colDefs={children}
        defaultOrderBy={defaultOrderBy}
        defaultOrderDirection={defaultOrderDirection}
        defaultRowsPerPage={defaultRowsPerPage}
    >
        <Paper>
            <TableContainer>
                <Table>
                    <KlabisTableHeaders/>
                    <KlabisTableBody rows={data?.data || []} onRowClick={onRowClick} emptyMessage={emptyMessage}/>
                </Table>
            </TableContainer>
            {<KlabisTablePagination itemsCount={data?.page.totalElements || data?.data.length || 0}/>}
        </Paper>
    </KlabisTableProvider>
};

/**
 const KlabisApiTable = <T extends Record<string, unknown>>(): ReactElement => {

 const [data, setData] = useState<TableData | undefined>(undefined);

 // Fetch data asynchronously via provided callback whenever paging changes
 useEffect(() => {
 let cancelled = false;
 (async () => {
 try {
 const result = await fetchData({page: tableContext.page, rowsPerPage: tableContext.rowsPerPage});
 if (!cancelled) setData(result);
 } catch (e) {
 if (!cancelled) setData(undefined);
 }
 })();
 return () => {
 cancelled = true;
 };
 }, [tableContext.page, tableContext.rowsPerPage, fetchData]);

 return <div></div>;
 }
 **/