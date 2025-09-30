import React from "react";

export interface TableCellRenderProps {
    item: any;
    column: string;
    value: any;
}

export interface TableCellProps {
    column: string;
    hidden?: boolean,
    sortable?: boolean,
    children: React.ReactNode;
    dataRender?: (props: TableCellRenderProps) => React.ReactNode;
}