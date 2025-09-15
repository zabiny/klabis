import React, {isValidElement, type ReactElement, type ReactNode} from 'react';
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

interface TableColumn {
    headerNode: ReactElement,
    column: string,
    hidden: boolean
}

const convertToTableColumn = (child: ReactNode): TableColumn | null => {
    if (isReactComponent(child)) {
        if (!isValidElement(child)) {
            return false;
        }

        const hidden = child.props?.hidden as boolean;
        const column = child.props?.column as string;

        return {
            headerNode: child,
            hidden: hidden, column: column
        } as TableColumn;
    }

    return null;
}

function isReactComponent(item: ReactNode): item is ReactElement {
    return (item as ReactElement).props !== undefined;
}

const KlabisTableInner = <T extends Record<string, any>>({
                                                             api,
                                                             children,
                                                             onRowClick,
                                                             queryKey,
                                                         }: KlabisTableProps<T>) => {
    const tableContext = useKlabisTableContext();
    const {data, isLoading, error} = useKlabisApi<T>(api, tableContext.createApiParams(), queryKey);

    // Funkce pro klonování dětských komponent s props
    const renderHeaderCells = () => {
        return React.Children.map(children, (child) => {
            const column = convertToTableColumn(child);
            if (column && !column.hidden) {
                return column.headerNode;
            } else {
                return null;
            }
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
                {React.Children
                    .map(children, (child) => {
                        const column = convertToTableColumn(child);
                        if (column && !column.hidden) {
                            const value = item[column.column];
                            return (
                                <MuiTableCell key={column.column}>
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