/**
 * Data loading wrapper for KlabisTable using React Query
 *
 * Handles fetching data from a HAL Link, managing pagination and sorting state,
 * building query parameters, and passing everything to the pure UI component.
 */

import {type ReactElement, useMemo, useState} from 'react'
import type {SortDirection} from '../../api'
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch'
import {usePersistedState} from '../../hooks/usePersistedState'
import {useTableSort, type SortState} from '../../hooks/useTableSort'
import {KlabisTable} from './KlabisTable'
import type {TablePageData} from './types'

/**
 * HAL Link object from API responses
 */
export interface Link {
    href: string
}

/**
 * Props for KlabisTableWithQuery component
 */
export interface KlabisTableWithQueryProps<T extends Record<string, unknown> = any> {
    // Data source (HAL Link)
    link: Link

    // Optional: Extract data from _embedded[collectionName] instead of content
    collectionName?: string

    // UI props (passed through to KlabisTable)
    onRowClick?: (item: T) => void
    defaultOrderBy?: string
    defaultOrderDirection?: SortDirection
    emptyMessage?: string
    hideEmptyColumns?: boolean
    rowsPerPageOptions?: number[]
    defaultRowsPerPage?: number

    /** Stable identifier for this table — enables sort persistence in localStorage */
    tableId?: string

    // Column definitions (TableCell components)
    children: React.ReactNode
}

interface KlabisTableCoreProps<T extends Record<string, unknown> = any> {
    link: Link
    collectionName?: string
    onRowClick?: (item: T) => void
    defaultOrderBy?: string
    defaultOrderDirection?: SortDirection
    emptyMessage?: string
    hideEmptyColumns?: boolean
    rowsPerPageOptions?: number[]
    defaultRowsPerPage?: number
    children: React.ReactNode
    sort: SortState | undefined
    onSortChange: (column: string, direction: SortDirection) => void
    onSortReset?: () => void
}

/**
 * Shared rendering logic for KlabisTableWithQuery.
 * Receives sort state from the caller so it can be backed by either useState or useTableSort.
 */
function KlabisTableCore<T extends Record<string, unknown> = any>({
    link,
    collectionName,
    onRowClick,
    defaultOrderBy,
    defaultOrderDirection = 'asc',
    emptyMessage,
    hideEmptyColumns,
    rowsPerPageOptions,
    defaultRowsPerPage = 10,
    children,
    sort,
    onSortChange,
    onSortReset,
}: KlabisTableCoreProps<T>): ReactElement {
    const [page, setPage] = useState(0)
    const [rowsPerPage, setRowsPerPage] = usePersistedState('klabis-table-rows-per-page', defaultRowsPerPage)

    const queryUrl = useMemo(() => {
        const url = new URL(link.href)
        url.searchParams.set('page', `${page}`)
        url.searchParams.set('size', `${rowsPerPage}`)

        url.searchParams.delete('sort')
        if (sort) {
            url.searchParams.append('sort', `${sort.by},${sort.direction}`)
        }

        return url.toString()
    }, [link.href, page, rowsPerPage, sort])

    const {data: response, error} = useAuthorizedQuery<any>(queryUrl, {
        staleTime: 30000,
        gcTime: 1000 * 60 * 5,
        retry: 1,
    })

    const tableData = useMemo(() => {
        if (!response) return []

        if (collectionName && response._embedded) {
            return (response._embedded[collectionName] as T[]) || []
        }

        return (response.content as T[]) || (response.data as T[]) || []
    }, [response, collectionName])

    const pageData: TablePageData | undefined = useMemo(() => {
        if (!response?.page) return undefined
        return response.page as TablePageData
    }, [response?.page])

    const handleSortChange = (column: string, direction: SortDirection) => {
        onSortChange(column, direction)
        setPage(0)
    }

    return (
        <KlabisTable<T>
            data={tableData}
            page={pageData}
            error={error}
            onSortChange={handleSortChange}
            onSortReset={onSortReset}
            onPageChange={setPage}
            onRowsPerPageChange={(newRowsPerPage) => {
                setRowsPerPage(newRowsPerPage)
                setPage(0)
            }}
            onRowClick={onRowClick}
            currentPage={page}
            rowsPerPage={rowsPerPage}
            currentSort={sort}
            defaultOrderBy={defaultOrderBy}
            defaultOrderDirection={defaultOrderDirection}
            emptyMessage={emptyMessage}
            hideEmptyColumns={hideEmptyColumns}
            rowsPerPageOptions={rowsPerPageOptions}
        >
            {children}
        </KlabisTable>
    )
}

/**
 * Variant that persists sort to localStorage via useTableSort.
 * Only rendered when tableId AND defaultOrderBy are provided.
 */
interface KlabisTableWithSortPersistenceProps<T extends Record<string, unknown> = any>
    extends Omit<KlabisTableWithQueryProps<T>, 'defaultOrderBy'> {
    tableId: string
    defaultOrderBy: string
}

function KlabisTableWithSortPersistence<T extends Record<string, unknown> = any>({
    tableId,
    defaultOrderBy,
    defaultOrderDirection = 'asc',
    ...rest
}: KlabisTableWithSortPersistenceProps<T>): ReactElement {
    const defaultSort: SortState = {by: defaultOrderBy, direction: defaultOrderDirection}
    const {sort, setSort, reset} = useTableSort(tableId, defaultSort)

    return (
        <KlabisTableCore<T>
            {...rest}
            defaultOrderBy={defaultOrderBy}
            defaultOrderDirection={defaultOrderDirection}
            sort={sort}
            onSortChange={(column, direction) => setSort({by: column, direction})}
            onSortReset={reset}
        />
    )
}

/**
 * Variant that uses plain useState for sort — no localStorage persistence.
 * Used for tables without a tableId, or when defaultOrderBy is absent.
 */
function KlabisTableWithLocalSort<T extends Record<string, unknown> = any>({
    defaultOrderBy,
    defaultOrderDirection = 'asc',
    ...rest
}: KlabisTableWithQueryProps<T>): ReactElement {
    const [sort, setSort] = useState<SortState | undefined>(
        defaultOrderBy ? {by: defaultOrderBy, direction: defaultOrderDirection} : undefined
    )

    return (
        <KlabisTableCore<T>
            {...rest}
            defaultOrderBy={defaultOrderBy}
            defaultOrderDirection={defaultOrderDirection}
            sort={sort}
            onSortChange={(column, direction) => setSort({by: column, direction})}
        />
    )
}

/**
 * Data loading wrapper for KlabisTable
 *
 * When `tableId` and `defaultOrderBy` are both provided, sort preference is persisted
 * to localStorage (key: `klabis.table.<tableId>.sort`). URL sort param always wins.
 *
 * Without `tableId`, behaves exactly as before — sort state is local only.
 *
 * @example
 * // With persistence
 * <KlabisTableWithQuery
 *   tableId="members"
 *   link={{href: '/api/members'}}
 *   defaultOrderBy="lastName"
 *   defaultOrderDirection="asc"
 * >
 *   <TableCell column="lastName" sortable>Příjmení</TableCell>
 * </KlabisTableWithQuery>
 */
export function KlabisTableWithQuery<T extends Record<string, unknown> = any>(
    props: KlabisTableWithQueryProps<T>
): ReactElement {
    const {tableId, defaultOrderBy} = props

    if (tableId && defaultOrderBy) {
        return <KlabisTableWithSortPersistence<T> {...props} tableId={tableId} defaultOrderBy={defaultOrderBy} />
    }

    return <KlabisTableWithLocalSort<T> {...props} />
}
