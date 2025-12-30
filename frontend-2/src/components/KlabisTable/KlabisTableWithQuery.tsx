/**
 * Data loading wrapper for KlabisTable using React Query
 *
 * Handles fetching data from a HAL Link, managing pagination and sorting state,
 * building query parameters, and passing everything to the pure UI component.
 */

import {type ReactElement, useMemo, useState} from 'react'
import type {SortDirection} from '../../api'
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch'
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
    rowsPerPageOptions?: number[]
    defaultRowsPerPage?: number

    // Column definitions (TableCell components)
    children: React.ReactNode
}

/**
 * Data loading wrapper for KlabisTable
 *
 * Fetches data from a HAL Link using useAuthorizedQuery and manages:
 * - Pagination state (page, rowsPerPage)
 * - Sort state (column, direction)
 * - Query parameter building (page, size, sort)
 * - Data extraction from HAL responses (content or _embedded[collectionName])
 *
 * @example
 * // Fetch from API with pagination and sorting
 * <KlabisTableWithQuery
 *   link={{href: '/api/members?page=0&size=10'}}
 *   defaultOrderBy="lastName"
 *   defaultOrderDirection="asc"
 *   onRowClick={(member) => navigate(`/members/${member.id}`)}
 * >
 *   <TableCell column="firstName" sortable>First Name</TableCell>
 *   <TableCell column="lastName" sortable>Last Name</TableCell>
 * </KlabisTableWithQuery>
 *
 * @example
 * // Fetch from _embedded collection
 * <KlabisTableWithQuery
 *   link={{href: '/api/organization'}}
 *   collectionName="membersList"
 * >
 *   <TableCell column="name">Name</TableCell>
 * </KlabisTableWithQuery>
 */
export function KlabisTableWithQuery<T extends Record<string, unknown> = any>({
                                                                                  link,
                                                                                  collectionName,
                                                                                  onRowClick,
                                                                                  defaultOrderBy,
                                                                                  defaultOrderDirection = 'asc',
                                                                                  emptyMessage,
                                                                                  rowsPerPageOptions,
                                                                                  defaultRowsPerPage = 10,
                                                                                  children,
                                                                              }: KlabisTableWithQueryProps<T>): ReactElement {
    // Pagination state
    const [page, setPage] = useState(0)
    const [rowsPerPage, setRowsPerPage] = useState(defaultRowsPerPage)

    // Sort state
    const [sort, setSort] = useState(
        defaultOrderBy
            ? {by: defaultOrderBy, direction: defaultOrderDirection}
            : undefined
    )

    // Build query URL with pagination and sort parameters
    const queryUrl = useMemo(() => {
        const url = new URL(link.href)
        url.searchParams.set('page', `${page}`)
        url.searchParams.set('size', `${rowsPerPage}`)

        // Clear any existing sort params and add new ones
        url.searchParams.delete('sort')
        if (sort) {
            url.searchParams.append('sort', `${sort.by},${sort.direction}`)
        }

        return url.toString()
    }, [link.href, page, rowsPerPage, sort])

    // Fetch data with React Query
    const {data: response, error} = useAuthorizedQuery<any>(queryUrl, {
        staleTime: 30000, // 30 seconds
        gcTime: 1000 * 60 * 5, // 5 minutes (formerly cacheTime)
        retry: 1,
    })

    // Extract data from response
    const tableData = useMemo(() => {
        if (!response) {
            return []
        }

        // Extract from _embedded[collectionName] if provided
        if (collectionName && response._embedded) {
            return (response._embedded[collectionName] as T[]) || []
        }

        // Otherwise use content or data field
        return (response.content as T[]) || (response.data as T[]) || []
    }, [response, collectionName])

    // Extract page metadata
    const pageData: TablePageData | undefined = useMemo(() => {
        if (!response?.page) {
            return undefined
        }
        return response.page as TablePageData
    }, [response?.page])

    // Handle sort change
    const handleSortChange = (column: string, direction: SortDirection) => {
        setSort({by: column, direction})
        setPage(0) // Reset to first page on sort change
    }

    // Handle page change
    const handlePageChange = (newPage: number) => {
        setPage(newPage)
    }

    // Handle rows per page change
    const handleRowsPerPageChange = (newRowsPerPage: number) => {
        setRowsPerPage(newRowsPerPage)
        setPage(0) // Reset to first page on rows per page change
    }

    return (
        <KlabisTable<T>
            data={tableData}
            page={pageData}
            error={error}
            onSortChange={handleSortChange}
            onPageChange={handlePageChange}
            onRowsPerPageChange={handleRowsPerPageChange}
            onRowClick={onRowClick}
            currentPage={page}
            rowsPerPage={rowsPerPage}
            currentSort={sort}
            defaultOrderBy={defaultOrderBy}
            defaultOrderDirection={defaultOrderDirection}
            emptyMessage={emptyMessage}
            rowsPerPageOptions={rowsPerPageOptions}
        >
            {children}
        </KlabisTable>
    )
}
