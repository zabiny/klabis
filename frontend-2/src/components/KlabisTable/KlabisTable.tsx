import React, {cloneElement, isValidElement, useState} from 'react';
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
import {SortableTableCell} from './SortableTableCell';

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

interface TableApiParams {
    size: number,
    sort: string[],
    page: number,
    additionalParams: Record<string, any>
}

export const KlabisTable = <T extends Record<string, any>>({
                                                               api,
                                                               children,
                                                               onRowClick,
                                                               defaultOrderBy,
                                                               defaultOrderDirection = 'asc',
                                                               defaultRowsPerPage = 10,
                                                               additionalParams = {},
                                                               queryKey,
                                                           }: KlabisTableProps<T>) => {
    const [page, setPage] = useState(0);
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage);
    const [orderBy, setOrderBy] = useState<string | undefined>(defaultOrderBy);
    const [orderDirection, setOrderDirection] = useState<SortDirection>(defaultOrderDirection);

    // Vytvoření parametrů pro API volání
    const apiParams = {
        page,
        size: rowsPerPage,
        sort: orderBy ? [`${orderBy},${orderDirection}`] : [],
        ...additionalParams,
    };

    const {data, isLoading, error} = useKlabisApi<T>(api, apiParams, queryKey);

    const handleChangePage = (event: unknown, newPage: number) => {
        setPage(newPage);
    };

    const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
        setRowsPerPage(parseInt(event.target.value, 10));
        setPage(0);
    };

    const handleRequestSort = (column: string) => {
        const isAsc = orderBy === column && orderDirection === 'asc';
        setOrderDirection(isAsc ? 'desc' : 'asc');
        setOrderBy(column);
        setPage(0);
    };

    // Funkce pro klonování dětských komponent s props
    const renderHeaderCells = () => {
        return React.Children.map(children, (child) => {
            if (isValidElement(child)) {
                if (child.type === SortableTableCell) {
                    return cloneElement(child as React.ReactElement<any>, {
                        orderBy,
                        orderDirection,
                        onRequestSort: handleRequestSort,
                    });
                }
                return child;
            }
            return child;
        });
    };

    console.table(data);

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
                rowsPerPage={rowsPerPage}
                page={page}
                onPageChange={handleChangePage}
                onRowsPerPageChange={handleChangeRowsPerPage}
                labelRowsPerPage="Řádků na stránku:"
                labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
            />
        </Paper>
    );
};

