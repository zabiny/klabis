import React from 'react'
import {useKlabisTableContext} from './KlabisTableContext'
import {TableHeaderCell} from './TableHeaderCell'
import type {TableCellProps} from './types'

/**
 * TableCell - Column definition component
 * Used declaratively to define table columns
 * Renders as a TableHeaderCell in the table header
 */
export const TableCell: React.FC<TableCellProps> = ({
                                                        column,
                                                        children,
                                                        hidden = false,
                                                        sortable = false
                                                    }) => {
    const {sort, handleRequestSort} = useKlabisTableContext()

    if (hidden) {
        return <></>
    }

    const isSorted = sort?.by === column
    const sortDirection = sort?.direction

    return (
        <TableHeaderCell
            sortable={sortable}
            isSorted={isSorted}
            sortDirection={sortDirection}
            onSort={() => handleRequestSort(column)}
        >
            {children}
        </TableHeaderCell>
    )
}

