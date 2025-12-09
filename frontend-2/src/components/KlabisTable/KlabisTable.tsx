import React, {ReactElement, ReactNode, useEffect} from 'react';
// Import pro MuiTableCell
import {
    Alert,
    CircularProgress,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TablePagination,
    TableRow,
} from '@mui/material';
import {type KlabisApiGetPaths, type SortDirection, useKlabisApiQuery} from '../../api';
import {KlabisTableProvider, useKlabisTableContext} from "./KlabisTableContext.tsx";

const KlabisTablePagination = ({totalElements}: { totalElements?: number }): ReactElement => {
    const tableContext = useKlabisTableContext();
    return <TablePagination
        rowsPerPageOptions={[5, 10, 25, 50]}
        component="div"
        count={totalElements || 0}
        rowsPerPage={tableContext.rowsPerPage} x
        page={tableContext.page}
        onPageChange={tableContext.handleChangePage}
        onRowsPerPageChange={tableContext.handleChangeRowsPerPage}
        labelRowsPerPage="Řádků na stránku:"
        labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
    />;
}

interface KlabisTableProps<T = any> {
    api: KlabisApiGetPaths;
    children: React.ReactNode;
    onRowClick?: (item: T) => void;
    // can be used to update UI based on klabis actions loaded in API used from Klabis Table.
    onTableActionsLoaded?: (actions: string[]) => void,
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    defaultRowsPerPage?: number;
    additionalParams?: Record<string, any>;
    emptyMessage?: string;
}

const KlabisTableInner = <T extends Record<string, any>>({
                                                             api,
                                                             onRowClick,
                                                             emptyMessage = "Žádná data",
                                                             onTableActionsLoaded = (actions) => {
                                                             }
                                                         }: KlabisTableProps<T>) => {
    const tableContext = useKlabisTableContext();
    const tableModel = tableContext.tableModel;

    const {
        data,
        isLoading,
        error,
        isSuccess
    } = useKlabisApiQuery("get", api, {params: {query: tableContext.createApiParams()}})

    useEffect(() => {
        if (isSuccess) {
            onTableActionsLoaded(data?._actions || []);
        }
    }, [isSuccess, onTableActionsLoaded, data]);

    const renderRows = (rows: object[]): ReactNode => {
        if (isLoading) {
            return (
                <TableRow key={0}><TableCell align={"center"}
                                             colSpan={tableContext.columnsCount}><CircularProgress/></TableCell></TableRow>
            );
        }

        if (error) {
            return (
                <TableRow key={0}><TableCell align={"center"}
                                             colSpan={tableContext.columnsCount}>
                    <Alert severity="error">
                        Nepodařilo se načíst data. Zkuste to prosím později. ({JSON.stringify(error)})
                    </Alert>
                </TableCell></TableRow>
            );
        }

        if (!rows || rows.length == 0) {
            return (
                <TableRow key={0}><TableCell align={"center"}
                                             colSpan={tableContext.columnsCount}>{emptyMessage}</TableCell></TableRow>
            );
        }

        return rows
            .map((item, index) => (
                <TableRow
                    key={item.id || index}
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
                        {renderRows(data?.content)}
                    </TableBody>
                </Table>
            </TableContainer>
            <KlabisTablePagination totalElements={data?.page?.totalElements}/>
        </Paper>
    );
};

export const KlabisTable = <T extends Record<string, any>>(props: KlabisTableProps<T>) => {
    return <KlabisTableProvider {...props} colDefs={props.children}>
        <KlabisTableInner {...props}/>
    </KlabisTableProvider>
};