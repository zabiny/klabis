import React from 'react';
import {TableCell as MuiTableCell} from '@mui/material';

interface TableCellProps {
    column: string;
    children: React.ReactNode;
}

export const TableCell: React.FC<TableCellProps> = ({children}) => {
    return <MuiTableCell>{children}</MuiTableCell>;
};
