import type {ReactElement, ReactNode} from 'react'
import React, {isValidElement, useMemo} from 'react'
import type {KlabisTableProps, TableCellProps, TableCellRenderProps} from './types'
import {Pagination} from './Pagination'
import type {SortDirection} from '../../api'

// Column definition extracted from children
interface ColumnDef {
    name: string
    label: ReactNode
    hidden: boolean
    sortable: boolean
    dataRender?: (props: TableCellRenderProps) => ReactNode
}

// Extract column definitions from children (TableCell components)
const extractColumns = (children: ReactNode): ColumnDef[] => {
    const columns: ColumnDef[] = []

    React.Children.forEach(children, (child) => {
        if (isValidElement(child)) {
            const props = child.props as Partial<TableCellProps>
            if (props.column) {
                columns.push({
                    name: props.column,
                    label: props.children,
                    hidden: props.hidden || false,
                    sortable: props.sortable || false,
                    dataRender: props.dataRender
                })
            }
        }
    })

    return columns
}

// Default cell render function
const defaultRenderCell = (props: TableCellRenderProps): ReactNode => (
    <span>{String(props.value)}</span>
)

// Add rowsPerPage to options if it's not already there
const ensurePageSizeInOptions = (pageSize: number, options: number[]): number[] => {
    if (options.includes(pageSize)) {
        return options
    }
    return [...options, pageSize].sort((a, b) => a - b)
}

// Error display component
const ErrorAlert = ({error}: { error: Error }) => (
    <div className="rounded-md border border-red-300 bg-red-50 p-4 text-red-800">
        <h3 className="font-semibold">Failed to load data</h3>
        <p className="text-sm">{error.message}</p>
    </div>
)

/**
 * Pure UI table component
 *
 * This is a presentation-only component that renders table UI.
 * All state management (pagination, sorting, data fetching) is handled by parent components.
 *
 * @example
 * // Basic usage with controlled state
 * function MyTable() {
 *   const [page, setPage] = useState(0);
 *   const [sort, setSort] = useState<SortState | undefined>();
 *
 *   return (
 *     <KlabisTable
 *       data={tableData}
 *       page={pageInfo}
 *       currentPage={page}
 *       onPageChange={setPage}
 *       onSortChange={(col, dir) => setSort({ by: col, direction: dir })}
 *       currentSort={sort}
 *     >
 *       <TableCell column="name" sortable>Name</TableCell>
 *       <TableCell column="email">Email</TableCell>
 *     </KlabisTable>
 *   );
 * }
 */
export const KlabisTable = <T extends Record<string, unknown>>({
                                                                   data,
                                                                   page,
                                                                   error,
                                                                   onSortChange,
                                                                   onPageChange,
                                                                   onRowsPerPageChange,
                                                                   onRowClick,
                                                                   children,
    defaultOrderBy,
    defaultOrderDirection = 'asc',
                                                                   emptyMessage = 'Žádná data',
                                                                   rowsPerPageOptions = [5, 10, 25, 50],
                                                                   rowsPerPage = 10,
                                                                   currentPage = 0,
                                                                   currentSort
                                                               }: KlabisTableProps<T>): ReactElement => {
    // Extract columns from JSX children
    const columns = useMemo(() => extractColumns(children), [children])

    // Get visible columns
    const visibleColumns = useMemo(
        () => columns.filter(col => !col.hidden),
        [columns]
    )

    // Effective rows per page options (ensure current size is in list)
    const effectiveRowsPerPageOptions = useMemo(
        () => ensurePageSizeInOptions(rowsPerPage, rowsPerPageOptions),
        [rowsPerPage, rowsPerPageOptions]
    )

    // Handle sort change
    const handleSort = (column: string) => {
        if (!onSortChange) return

        // Determine new sort direction
        let newDirection: SortDirection = 'asc'
        if (currentSort?.by === column && currentSort.direction === 'asc') {
            newDirection = 'desc'
        }

        onSortChange(column, newDirection)
    }

    // Handle pagination change
    const handlePagingChange = (newPage: number, newRowsPerPage: number) => {
        onPageChange?.(newPage)
        onRowsPerPageChange?.(newRowsPerPage)
    }

    // Render error state if present
    if (error) {
        return (
            <div className="rounded-md border border-border bg-surface-raised p-4">
                <ErrorAlert error={error}/>
            </div>
        )
    }

    const rows = data || []

    return (
        <div className="shadow-md rounded-md overflow-hidden border border-border bg-surface-raised">
            {/* Table */}
            <div className="overflow-x-auto">
                <table className="w-full" aria-label="Tabulka dat">
                    {/* Header */}
                    <thead>
                    <tr className="bg-surface-base border-b border-border">
                        {visibleColumns.map((col) => (
                            <th
                                key={col.name}
                                className="px-4 py-3 text-left text-sm font-semibold text-text-primary"
                            >
                                {col.sortable ? (
                                    <button
                                        className="inline-flex items-center gap-2 cursor-pointer hover:text-text-secondary"
                                        onClick={() => handleSort(col.name)}
                                        aria-sort={currentSort?.by === col.name ? (currentSort.direction === 'asc' ? 'ascending' : 'descending') : 'none'}
                                        aria-label={`Sort by ${String(col.label)}${currentSort?.by === col.name ? ` (${currentSort.direction})` : ''}`}
                                    >
                                        {col.label}
                                        {currentSort?.by === col.name && (
                                            <span className="text-xs" aria-hidden="true">
                                                    {currentSort.direction === 'asc' ? '↑' : '↓'}
                                                </span>
                                        )}
                                    </button>
                                ) : (
                                    col.label
                                )}
                            </th>
                        ))}
                    </tr>
                    </thead>

                    {/* Body */}
                    <tbody>
                    {rows.length === 0 ? (
                        <tr>
                            <td
                                colSpan={visibleColumns.length}
                                className="px-4 py-8 text-center text-sm text-text-secondary"
                            >
                                {emptyMessage}
                            </td>
                        </tr>
                    ) : (
                        <>
                            {rows.map((item, rowIndex) => (
                                <tr
                                    key={item.id ? String(item.id) : `row-${rowIndex}`}
                                    className={`border-b border-border transition-colors ${
                                        onRowClick
                                            ? 'hover:bg-surface-base cursor-pointer'
                                            : ''
                                    }`}
                                    onClick={() => onRowClick?.(item)}
                                    onKeyDown={(e) => {
                                        if ((e.key === 'Enter' || e.key === ' ') && onRowClick) {
                                            e.preventDefault()
                                            onRowClick(item)
                                        }
                                    }}
                                    tabIndex={onRowClick ? 0 : -1}
                                    role={onRowClick ? 'button' : undefined}
                                    aria-label={`Row ${rowIndex + 1}`}
                                >
                                    {visibleColumns.map((col) => {
                                        const value = item[col.name]
                                        const renderFn = col.dataRender || defaultRenderCell
                                        let cellContent: ReactNode
                                        try {
                                            cellContent = renderFn({
                                                item,
                                                column: col.name,
                                                value
                                            })
                                        } catch (error) {
                                            console.error(`Error rendering cell for column "${col.name}":`, error)
                                            cellContent = <span className="text-red-500 text-xs">Error</span>
                                        }

                                        return (
                                            <td
                                                key={col.name}
                                                className="px-4 py-3 text-sm text-text-primary"
                                            >
                                                {cellContent}
                                            </td>
                                        )
                                    })}
                                </tr>
                            ))}
                        </>
                    )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            {page && (
                <Pagination
                    count={page.totalElements}
                    page={currentPage}
                    rowsPerPage={rowsPerPage}
                    onPageChange={(newPage) => handlePagingChange(newPage, rowsPerPage)}
                    onRowsPerPageChange={(newRowsPerPage) => handlePagingChange(currentPage, newRowsPerPage)}
                    rowsPerPageOptions={effectiveRowsPerPageOptions}
                    labelRowsPerPage="Řádků na stránku:"
                    labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
                />
            )}
        </div>
    )
}
