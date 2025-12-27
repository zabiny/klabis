import type {ReactElement, ReactNode} from 'react'
import React, {isValidElement, useEffect, useMemo, useRef, useState} from 'react'
import type {KlabisTableProps, TableCellProps, TableCellRenderProps, TableData} from './types'
import {Pagination} from './Pagination'
import type {PaginatedApiParams} from '../../api'

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

export const KlabisTable = <T extends Record<string, unknown>>({
                                                                   data: staticData,
                                                                   page: staticPage,
                                                                   fetchData,
                                                                   onStateChange,
    children,
                                                                   onRowClick,
    defaultOrderBy,
    defaultOrderDirection = 'asc',
    defaultRowsPerPage = 10,
                                                                   emptyMessage = 'Žádná data',
                                                                   rowsPerPageOptions = [5, 10, 25, 50]
                                                               }: KlabisTableProps<T>): ReactElement => {
    // State management
    const [page, setPage] = useState(0)
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage)
    const [sort, setSort] = useState(defaultOrderBy ? {
        by: defaultOrderBy,
        direction: defaultOrderDirection
    } : undefined)
    const [tableData, setTableData] = useState<TableData<T>>(
        staticData ? {data: staticData, page: staticPage} : {data: []}
    )

    // Extract columns from JSX children
    const columns = useMemo(() => extractColumns(children), [children])

    // Sync static data and rowsPerPage with page.size
    useEffect(() => {
        if (staticData) {
            setTableData({data: staticData, page: staticPage})
            // Sync rowsPerPage immediately when staticPage changes
            if (staticPage?.size && staticPage.size !== rowsPerPage) {
                setRowsPerPage(staticPage.size)
            }
        }
    }, [staticData, staticPage, rowsPerPage])

    // Fetch data when state changes (if fetchData provided)
    useEffect(() => {
        if (!fetchData) {
            return
        }

        let cancelled = false

        const performFetch = async () => {
            try {
                const apiParams: PaginatedApiParams = {
                    page,
                    size: rowsPerPage,
                    sort: sort ? [`${sort.by},${sort.direction}`] : []
                }

                const result = await fetchData(apiParams)
                if (!cancelled) {
                    setTableData(result)
                }
            } catch (e) {
                if (!cancelled) {
                    setTableData({data: []})
                    console.error('Failed to fetch table data:', e)
                }
            }
        }

        performFetch()

        return () => {
            cancelled = true
        }
    }, [fetchData, page, rowsPerPage, sort])

    // Keep onStateChange in a ref to avoid dependency on its identity
    const onStateChangeRef = useRef(onStateChange)
    useEffect(() => {
        onStateChangeRef.current = onStateChange
    }, [onStateChange])

    // Notify parent of state changes
    useEffect(() => {
        if (onStateChangeRef.current) {
            onStateChangeRef.current({page, rowsPerPage, sort})
        }
    }, [page, rowsPerPage, sort])

    // Handle sort change
    const handleSort = (column: string) => {
        setSort((prevSort) => {
            if (!prevSort || prevSort.by !== column) {
                return {by: column, direction: 'asc'}
            } else {
                return {by: column, direction: prevSort.direction === 'asc' ? 'desc' : 'asc'}
            }
        })
        setPage(0)
    }

    // Handle pagination change
    const handlePagingChange = (newPage: number, newRowsPerPage: number) => {
        setPage(newPage)
        setRowsPerPage(newRowsPerPage)
    }

    // Effective rows per page options (ensure current size is in list)
    const effectiveRowsPerPageOptions = useMemo(
        () => ensurePageSizeInOptions(rowsPerPage, rowsPerPageOptions),
        [rowsPerPage, rowsPerPageOptions]
    )

    // Get visible columns
    const visibleColumns = useMemo(
        () => columns.filter(col => !col.hidden),
        [columns]
    )

    const rows = tableData.data || []
    const pageData = tableData.page

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
                                        aria-sort={sort?.by === col.name ? (sort.direction === 'asc' ? 'ascending' : 'descending') : 'none'}
                                        aria-label={`Sort by ${String(col.label)}${sort?.by === col.name ? ` (${sort.direction})` : ''}`}
                                    >
                                        {col.label}
                                        {sort?.by === col.name && (
                                            <span className="text-xs" aria-hidden="true">
                                                    {sort.direction === 'asc' ? '↑' : '↓'}
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
                        rows.map((item, rowIndex) => (
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
                                aria-label={`Řádek ${rowIndex + 1}`}
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
                        ))
                    )}
                    </tbody>
                </table>
            </div>

            {/* Pagination */}
            {pageData && (
                <Pagination
                    count={pageData.totalElements}
                    page={pageData.number}
                    rowsPerPage={rowsPerPage}
                    onPageChange={(newPage) => handlePagingChange(newPage, rowsPerPage)}
                    onRowsPerPageChange={(newRowsPerPage) => handlePagingChange(pageData.number, newRowsPerPage)}
                    rowsPerPageOptions={effectiveRowsPerPageOptions}
                    labelRowsPerPage="Řádků na stránku:"
                    labelDisplayedRows={({from, to, count}) => `${from}-${to} z ${count}`}
                />
            )}
        </div>
    )
}
