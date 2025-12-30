import {KlabisTableWithQuery, type Link} from '../KlabisTable'
import {type ReactNode} from 'react'
import {toHref, useHalExplorerNavigation} from './hooks'

interface HalNavigatorTableProps<T extends Record<string, unknown>> {
    embeddedName: string
    children: React.ReactNode
    onRowClick?: (item: T) => void
    defaultOrderBy?: string
    defaultOrderDirection?: 'asc' | 'desc'
    emptyMessage?: string
    defaultRowsPerPage?: number
}

// TODO: cleanup these old HalNavigator classes. Only thing what is left to migrate are customized forms

/**
 * Table component for displaying HAL embedded collections in the old HalNavigator context
 *
 * Uses KlabisTableWithQuery to handle data fetching, pagination, and sorting
 * from the current HAL navigation context.
 */
export const HalNavigatorTable = <T extends Record<string, unknown>>({
                                                                         embeddedName,
                                                                         ...tableProps
                                                                     }: HalNavigatorTableProps<T>): ReactNode => {
    const navigation = useHalExplorerNavigation()

    // Get the current HAL resource URL
    const href = toHref(navigation.current)
    if (!href) {
        return (
            <div className="rounded-md border border-red-300 bg-red-50 p-4 text-red-800">
                <h3 className="font-semibold">Failed to load data</h3>
                <p className="text-sm">No resource URL available</p>
            </div>
        )
    }

    const link: Link = {href}

    return (
        <KlabisTableWithQuery<T>
            link={link}
            collectionName={embeddedName}
            {...tableProps}
        />
    )
}
