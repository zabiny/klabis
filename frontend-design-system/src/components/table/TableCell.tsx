import React from 'react'
import type {TableCellProps} from './types'

/**
 * TableCell - Column definition component
 *
 * Used declaratively to define table columns. This is a pure definition component
 * that doesn't render directly - the parent KlabisTable extracts column definitions
 * from children and uses them to render the actual table.
 *
 * @example
 * <KlabisTable>
 *   <TableCell column="name" sortable>Name</TableCell>
 *   <TableCell column="email">Email</TableCell>
 * </KlabisTable>
 */
export const TableCell: React.FC<TableCellProps> = () => {
    // This component doesn't render anything - it's only used for its props
    return null
}

TableCell.displayName = 'TableCell'
