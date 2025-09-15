import React from 'react';
import {TableCell as MuiTableCell} from '@mui/material';

interface TableCellProps {
    column: string;
    hidden?: boolean,
    children: React.ReactNode;
}

export const TableCell: React.FC<TableCellProps> = ({children, hidden}) => {

    if (hidden) {
        return <></>;
    }

    return <MuiTableCell>{children}</MuiTableCell>;
};
