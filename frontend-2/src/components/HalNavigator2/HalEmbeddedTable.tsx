/**
 * Component for displaying HAL _embedded collections as a table
 * Uses KlabisTableWithQuery to fetch and render data with pagination and sorting
 */

import {type ReactElement} from 'react'
import {useHalRoute} from '../../contexts/HalRouteContext'
import {KlabisTableWithQuery} from '../KlabisTable'
import {ErrorDisplay} from '../UI'
import {containerStyles, spinnerStyles} from '../../theme/designTokens'
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
 * and displays it using KlabisTableWithQuery with pagination and sorting support.
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
    const {getResourceLink, isLoading} = useHalRoute()

    // Extract self link from HAL resource
    const selfLink = getResourceLink();

    if (isLoading) {
        return (
            <div className={containerStyles.loadingContainer}>
                <div className={spinnerStyles.spinner}></div>
                <span className={spinnerStyles.loadingText}>Načítání dat tabulky...</span>
            </div>
        )
    }

    // Show error state if self link is not available
    if (!selfLink) {
        const error = new Error('Self link not found in resource data - cannot fetch table data')
        console.error('Failed to fetch table data:', error)
        return (
            <ErrorDisplay
                error={error}
                title="Failed to load data"
            />
        )
    }

    return (
        <KlabisTableWithQuery<T>
            link={selfLink}
            collectionName={collectionName}
            onRowClick={onRowClick}
            defaultOrderBy={defaultOrderBy}
            defaultOrderDirection={defaultOrderDirection}
            emptyMessage={emptyMessage}
        >
            {children}
        </KlabisTableWithQuery>
    )
}
