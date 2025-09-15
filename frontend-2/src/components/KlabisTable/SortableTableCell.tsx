import React from 'react';
import {TableCell as MuiTableCell, TableSortLabel} from '@mui/material';
import {useKlabisTableContext} from "./KlabisTableContext.tsx";

interface SortableTableCellProps {
    column: string;
    children: React.ReactNode
}

export const SortableTableCell: React.FC<SortableTableCellProps> = ({
                                                                        column,
                                                                        children,
                                                                    }) => {

    const tableContext = useKlabisTableContext();

    const handleSort = () => {
        tableContext.handleRequestSort(column);
    };

    return (
        <MuiTableCell>
            <TableSortLabel
                active={tableContext.orderBy === column}
                direction={tableContext.orderBy === column ? tableContext.orderDirection : 'asc'}
                onClick={handleSort}
                sx={{cursor: 'pointer'}}
            >
                {children}
            </TableSortLabel>
        </MuiTableCell>
    );
};
