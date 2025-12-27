/**
 * Component for displaying HAL _embedded collections as a table
 * Uses KlabisTable to render data with automatic column definitions
 */

import {type ReactElement, useMemo} from 'react'
import {useHalRoute} from '../../contexts/HalRouteContext'
import {KlabisTable} from '../KlabisTable'
import {type SortDirection} from '../../api'

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

    // Extract collection data and page info from HAL response
    const collectionData = useMemo(
        () => (resourceData?._embedded?.[collectionName] || []) as T[],
        [resourceData, collectionName]
    )

    const pageData = useMemo(
        () => resourceData?.page as any,
        [resourceData]
    )

    return (
        <KlabisTable<T>
            data={collectionData}
            page={pageData}
            onRowClick={onRowClick}
            defaultOrderBy={defaultOrderBy}
            defaultOrderDirection={defaultOrderDirection}
            emptyMessage={emptyMessage}
        >
            {children}
        </KlabisTable>
    )
}
