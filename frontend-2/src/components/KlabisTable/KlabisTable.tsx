import React, {ReactElement, ReactNode, useEffect, useState} from 'react';
// Import pro MuiTableCell
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow,} from '@mui/material';
import {type SortDirection} from '../../api';
import {KlabisTableProvider, useKlabisTableContext} from "./KlabisTableContext.tsx";
import {type FetchTableDataCallback, type TableData} from './types';

const KlabisTablePagination = ({totalElements}: { totalElements?: number }): ReactElement => {
    const tableContext = useKlabisTableContext();
    return <TablePagination
        rowsPerPageOptions={[5, 10, 25, 50]}
        component="div"
        count={totalElements || 0}
        rowsPerPage={tableContext.rowsPerPage}
        page={tableContext.page}
        onPageChange={tableContext.handleChangePage}
        onRowsPerPageChange={tableContext.handleChangeRowsPerPage}
        labelRowsPerPage="Řádků na stránku:"
        labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
    />;
}

interface KlabisTableProps<T = unknown> {
    fetchData: FetchTableDataCallback;
    children: React.ReactNode;
    onRowClick?: (item: T) => void;
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    defaultRowsPerPage?: number;
    emptyMessage?: string;
}

const KlabisTableInner = <T extends Record<string, unknown>>({
                                                                 fetchData,
                                                             onRowClick,
                                                             emptyMessage = "Žádná data",
                                                         }: KlabisTableProps<T>) => {
    const tableContext = useKlabisTableContext();
    const tableModel = tableContext.tableModel;
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

    const renderRows = (rows?: Record<string, unknown>[]): ReactNode => {
        if (!rows || rows.length == 0) {
            return (
                <TableRow key={0}><TableCell align={"center"}
                                             colSpan={tableContext.columnsCount}>{emptyMessage}</TableCell></TableRow>
            );
        }

        return rows
            .map((item, index) => (
                <TableRow
                    key={item?.id || index}
                    hover
                    onClick={() => onRowClick?.(item)}
                    sx={{cursor: onRowClick ? 'pointer' : 'default'}}
                >
                    {tableModel.renderCellsForRow(item)}
                </TableRow>
            ));
    };


    return (
        <Paper>
            <TableContainer>
                <Table>
                    <TableHead>
                        <TableRow>
                            {tableModel.renderHeaders()}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {renderRows(data?.data)}
                    </TableBody>
                </Table>
            </TableContainer>
            <KlabisTablePagination totalElements={data?.page?.totalElements}/>
        </Paper>
    );
};

export const KlabisTable = <T extends Record<string, unknown>>(props: KlabisTableProps<T>) => {
    const {
        children,
        defaultOrderBy,
        defaultOrderDirection,
        defaultRowsPerPage,
        fetchData,
        onRowClick,
        emptyMessage
    } = props;

    return <KlabisTableProvider
        colDefs={children}
        defaultOrderBy={defaultOrderBy}
        defaultOrderDirection={defaultOrderDirection}
        defaultRowsPerPage={defaultRowsPerPage}
    >
        <KlabisTableInner
            fetchData={fetchData}
            onRowClick={onRowClick}
            emptyMessage={emptyMessage}
            children={children}
        />
    </KlabisTableProvider>
};