import React, {isValidElement} from 'react';
// Import pro MuiTableCell
import {
    Alert,
    Box,
    CircularProgress,
    Paper,
    Table,
    TableBody,
    TableCell as MuiTableCell,
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
    defaultOrderBy?: string;
    defaultOrderDirection?: SortDirection;
    defaultRowsPerPage?: number;
    additionalParams?: Record<string, any>;
    queryKey?: string;
}

const KlabisTableInner = <T extends Record<string, any>>({
                                                             api,
                                                             children,
                                                             onRowClick,
                                                             queryKey,
                                                         }: KlabisTableProps<T>) => {
    const tableContext = useKlabisTableContext();
    console.table(tableContext);

    const {data, isLoading, error} = useKlabisApi<T>(api, tableContext.createApiParams(), queryKey);

    // Funkce pro klonování dětských komponent s props
    const renderHeaderCells = () => {
        return React.Children.map(children, (child) => {
            if (isValidElement(child)) {
                return child;
            }
            return child;
        });
    };

    // Funkce pro vykreslení dat v řádcích
    const renderTableRows = () => {
        if (!data?.data.content) return null;

        return data.data.content.map((item, index) => (
            <TableRow
                key={item.id || index}
                hover
                onClick={() => onRowClick?.(item)}
                sx={{cursor: onRowClick ? 'pointer' : 'default'}}
            >
                {React.Children.map(children, (child) => {
                    if (isValidElement(child) && child.props.column) {
                        const value = item[child.props.column];
                        return (
                            <MuiTableCell key={child.props.column}>
                                {value}
                            </MuiTableCell>
                        );
                    }
                    return null;
                })}
            </TableRow>
        ));
    };

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

    return (
        <Paper>
            <TableContainer>
                <Table>
                    <TableHead>
                        <TableRow>
                            {renderHeaderCells()}
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {renderTableRows()}
                    </TableBody>
                </Table>
            </TableContainer>
            <TablePagination
                rowsPerPageOptions={[5, 10, 25, 50]}
                component="div"
                count={data?.data.page.totalElements || 0}
                rowsPerPage={tableContext.rowsPerPage}
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
    return <KlabisTableProvider {...props}>
        <KlabisTableInner {...props}/>
    </KlabisTableProvider>
};