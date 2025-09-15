import React from 'react';
import {TableCell as MuiTableCell, TableSortLabel} from '@mui/material';
import {type SortDirection} from './types';

interface SortableTableCellProps {
    column: string;
    children: React.ReactNode;
    orderBy?: string;
    orderDirection?: SortDirection;
    onRequestSort?: (column: string) => void;
}

export const SortableTableCell: React.FC<SortableTableCellProps> = ({
                                                                        column,
                                                                        children,
                                                                        orderBy,
                                                                        orderDirection,
                                                                        onRequestSort,
                                                                    }) => {
    const handleSort = () => {
        if (onRequestSort) {
            onRequestSort(column);
        }
    };

    return (
        <MuiTableCell>
            <TableSortLabel
                active={orderBy === column}
                direction={orderBy === column ? orderDirection : 'asc'}
                onClick={handleSort}
                sx={{cursor: 'pointer'}}
            >
                {children}
            </TableSortLabel>
        </MuiTableCell>
    );
};
