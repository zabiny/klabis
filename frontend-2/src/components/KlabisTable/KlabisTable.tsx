import React, {type ReactNode} from 'react';
// Import pro MuiTableCell
import {Paper, Table, TableBody, TableCell, TableContainer, TableHead, TablePagination, TableRow,} from '@mui/material';
import {type SortDirection} from '../../api';
import {KlabisTableProvider, useKlabisTableContext} from "./KlabisTableContext.tsx";
import {type FetchTableDataCallback} from './types';

const KlabisTablePagination = (): ReactNode => {
    const {paging, handlePagingChange: onPagingChange} = useKlabisTableContext();

    return paging && <TablePagination
        rowsPerPageOptions={[5, 10, 25, 50]}
        component="div"
        count={paging.totalElements}
        rowsPerPage={paging.size}
        page={paging.number}
        onPageChange={(_event: unknown, pageNumber: number) => onPagingChange(pageNumber, paging.size)}
        onRowsPerPageChange={(event) => onPagingChange(paging.number, parseInt(event.target.value, 10))}
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

const KlabisTableBody = <T extends Record<string, unknown>>({onRowClick, emptyMessage = "Žádná data"}: {
    onRowClick?: (item: T) => void,
    emptyMessage?: string
}): ReactNode => {
    const {tableModel, rows} = useKlabisTableContext<T>();

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

    return <KlabisTableProvider
        colDefs={children}
        fetchData={fetchData}
        defaultOrderBy={defaultOrderBy}
        defaultOrderDirection={defaultOrderDirection}
        defaultRowsPerPage={defaultRowsPerPage}
    >
        <Paper>
            <TableContainer>
                <Table>
                    <KlabisTableHeaders/>
                    <KlabisTableBody onRowClick={onRowClick} emptyMessage={emptyMessage}/>
                </Table>
            </TableContainer>
            {<KlabisTablePagination/>}
        </Paper>
    </KlabisTableProvider>
};