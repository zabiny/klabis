/**
 * Component for displaying HAL _embedded collections as a table
 * Uses KlabisTable to render data with automatic column definitions
 */

import {type ReactElement} from 'react'
import {useHalRoute} from '../../contexts/HalRouteContext'
import {type FetchTableDataCallback, KlabisTable} from '../KlabisTable'
import {type SortDirection} from '../../api'
import {fetchResource} from "../HalNavigator/hooks.ts";

/**
 * Props for HalEmbeddedTable component
 */
export interface HalEmbeddedTableProps<T = any> {
    /** Name of the collection in _embedded object */
    collectionName: string

    /** Callback when a row is clicked */
    onRowClick?: (item: T) => void

    /** Default column to sort by */
    defaultOrderBy?: string

    /** Default sort direction (asc or desc) */
    defaultOrderDirection?: SortDirection

    /** Message to display when no data is available */
    emptyMessage?: string

    /** Column definitions (TableCell components) */
    children: React.ReactNode
}

/**
 * Component for displaying HAL _embedded collections in a table format
 *
 * Automatically fetches data from the current resource's _embedded property
 * and displays it using KlabisTable with pagination and sorting support.
 *
 * @example
 * // Display members list from _embedded.membersApiResponseList
 * <HalEmbeddedTable
 *   collectionName="membersApiResponseList"
 *   defaultOrderBy="lastName"
 *   defaultOrderDirection="asc"
 *   onRowClick={(member) => navigate(`/members/${member.id}`)}
 * >
 *   <TableCell column="firstName" sortable>Jméno</TableCell>
 *   <TableCell column="lastName" sortable>Příjmení</TableCell>
 *   <TableCell column="registrationNumber">Reg. číslo</TableCell>
 * </HalEmbeddedTable>
 */
export function HalEmbeddedTable<T extends Record<string, unknown> = any>({
                                                                              collectionName,
                                                                              onRowClick,
                                                                              defaultOrderBy,
                                                                              defaultOrderDirection = 'asc',
                                                                              emptyMessage = 'Žádná data',
                                                                              children,
                                                                          }: HalEmbeddedTableProps<T>): ReactElement {
    const {resourceData} = useHalRoute()

    const fetchTableData: FetchTableDataCallback<T> = async (apiParams) => {
        const selfLink = resourceData?._links?.self
        if (!selfLink) {
            throw new Error('Self link not found in resource data - cannot fetch table data')
        }

        const href = Array.isArray(selfLink) ? selfLink[0]?.href : selfLink?.href
        if (!href) {
            throw new Error('Self link href is empty')
        }

        const targetUrl = new URL(href)
        targetUrl.searchParams.set('page', `${apiParams.page}`)
        targetUrl.searchParams.set('size', `${apiParams.size}`)
        targetUrl.searchParams.delete('sort');
        apiParams.sort.forEach((str) => targetUrl.searchParams.append('sort', str))

        const response = await fetchResource(targetUrl)
        return {
            // get data from given embedded relation name (should be same as initial data were)
            data: (response?._embedded?.[collectionName] as T[]) || [],
            page: response.page,
        }
    }

    return (
        <KlabisTable<T>
            fetchData={fetchTableData}
            onRowClick={onRowClick}
            defaultOrderBy={defaultOrderBy}
            defaultOrderDirection={defaultOrderDirection}
            emptyMessage={emptyMessage}
        >
            {children}
        </KlabisTable>
    )
}
