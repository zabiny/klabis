import React, {ReactNode, useEffect} from 'react';
// Import pro MuiTableCell
import {
    Alert,
    Box,
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
import {useKlabisApi} from './useKlabisApi';
import {type SortDirection} from './types';
import {KlabisTableProvider, useKlabisTableContext} from "./KlabisTableContext.tsx";

interface KlabisTableProps<T = any> {
    api: string;
    children: React.ReactNode;
    onRowClick?: (item: T) => void;
    // can be used to update UI based on klabis actions loaded in API used from Klabis Table.
    onTableActionsLoaded?: (actions: string[]) => void,
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    defaultRowsPerPage?: number;
    additionalParams?: Record<string, any>;
    queryKey?: string;
}

const KlabisTableInner = <T extends Record<string, any>>({
                                                             api,
                                                             onRowClick,
                                                             queryKey,
                                                             onTableActionsLoaded = (actions) => {
                                                             }
                                                         }: KlabisTableProps<T>) => {
    const tableContext = useKlabisTableContext();
    const {data, isLoading, error, isSuccess} = useKlabisApi<T>(api, tableContext.createApiParams(), queryKey);

    useEffect(() => {
        if (isSuccess) {
            onTableActionsLoaded(data?._actions || []);
        }
    }, [isSuccess, onTableActionsLoaded, data]);

    if (isLoading) {
        return (
            <Box sx={{display: 'flex', justifyContent: 'center', mt: 4}}>
                <CircularProgress/>
            </Box>
        );
    }

    if (error) {
        return (
            <Alert severity="error">
                Nepodařilo se načíst data. Zkuste to prosím později.
            </Alert>
        );
    }

    const tableModel = tableContext.tableModel;

    const renderRows = (rows: object[]): ReactNode => {
        if (!rows) return (
            <TableRow key={0}><TableCell align={"center"} colSpan={tableContext.columnsCount}>No
                content</TableCell></TableRow>
        );

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
            <TablePagination
                rowsPerPageOptions={[5, 10, 25, 50]}
                component="div"
                count={data?.page.totalElements || 0}
                rowsPerPage={tableContext.rowsPerPage} x
                page={tableContext.page}
                onPageChange={tableContext.handleChangePage}
                onRowsPerPageChange={tableContext.handleChangeRowsPerPage}
                labelRowsPerPage="Řádků na stránku:"
                labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
            />
        </Paper>
    );
};

export const KlabisTable = <T extends Record<string, any>>(props: KlabisTableProps<T>) => {
    return <KlabisTableProvider {...props} colDefs={props.children}>
        <KlabisTableInner {...props}/>
    </KlabisTableProvider>
};