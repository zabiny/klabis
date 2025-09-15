import React from 'react';
import {TableCell as MuiTableCell, TableSortLabel} from '@mui/material';
import {useKlabisTableContext} from "./KlabisTableContext.tsx";

interface SortableTableCellProps {
    column: string;
    hidden?: boolean,
    children: React.ReactNode
}

export const SortableTableCell: React.FC<SortableTableCellProps> = ({
                                                                        column,
                                                                        children,
                                                                        hidden
                                                                    }) => {
    const tableContext = useKlabisTableContext();

    if (hidden) {
        return <></>;
    }

    const handleSort = () => {
        tableContext.handleRequestSort(column);
    };

    return (
        <MuiTableCell hidden={hidden}>
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
