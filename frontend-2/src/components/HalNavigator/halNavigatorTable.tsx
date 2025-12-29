import {type FetchTableDataCallback, KlabisTable} from '../KlabisTable'
import {type ReactNode} from 'react'
import {fetchResource, toHref, useHalExplorerNavigation} from './hooks'

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

export const HalNavigatorTable = <T extends Record<string, unknown>>({
                                                                         embeddedName,
                                                                         ...tableProps
                                                                     }: HalNavigatorTableProps<T>): ReactNode => {
    const navigation = useHalExplorerNavigation()

    const fetchTableData: FetchTableDataCallback<T> = async (apiParams) => {
        const targetUrl = new URL(toHref(navigation.current))
        targetUrl.searchParams.append('page', `${apiParams.page}`)
        targetUrl.searchParams.append('size', `${apiParams.size}`)
        apiParams.sort.forEach((str) => targetUrl.searchParams.append('sort', str))

        const response = await fetchResource(targetUrl)
        return {
            // get data from given embedded relation name (should be same as initial data were)
            data: (response?._embedded?.[embeddedName] as T[]) || [],
            page: response.page,
        }
    }

    return <KlabisTable fetchData={fetchTableData} {...tableProps} />
}