import React from 'react';
import {TableCell as MuiTableCell, TableSortLabel} from '@mui/material';
import {useKlabisTableContext} from "./KlabisTableContext.tsx";
import {type TableCellProps} from "./types";

interface SortLabelProps {
    column: string,
    children: React.ReactNode
}

const SortLabel = ({column, children}: SortLabelProps) => {
    const tableContext = useKlabisTableContext();

    const handleSort = () => {
        tableContext.handleRequestSort(column);
    };

    return (<TableSortLabel
        active={tableContext.orderBy === column}
        direction={tableContext.orderBy === column ? tableContext.orderDirection : 'asc'}
        onClick={handleSort}
        sx={{cursor: 'pointer'}}
    >
        {children}
    </TableSortLabel>);
}

export const TableCell: React.FC<TableCellProps> = ({column, children, hidden = false, sortable = false}) => {

    if (hidden) {
        return <></>;
    }

    return <MuiTableCell>
        {sortable ? <SortLabel column={column}>{children}</SortLabel> : children}
    </MuiTableCell>;
};

