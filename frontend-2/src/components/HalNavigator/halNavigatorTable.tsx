import {type FetchTableDataCallback, KlabisTable, type KlabisTableProps} from "../KlabisTable";
import {type ReactNode} from "react";
import {fetchResource, toHref, useHalExplorerNavigation} from "./hooks";

export const HalNavigatorTable = <T extends Record<string, unknown>>({
                                                                         embeddedName,
                                                                         ...tableProps
                                                                     }: Omit<KlabisTableProps<T> & {
    embeddedName: string
}, "fetchData">): ReactNode => {

    const navigation = useHalExplorerNavigation();

    function tableDataFetcherFactory<T>(relName: string): FetchTableDataCallback<T> {
        return async (apiParams) => {
            const targetUrl = new URL(toHref(navigation.current));
            targetUrl.searchParams.append('page', `${apiParams.page}`)
            targetUrl.searchParams.append('size', `${apiParams.size}`)
            apiParams.sort.forEach(str => targetUrl.searchParams.append('sort', str));

            const response = await fetchResource(targetUrl);
            return {
                // get data from given embedded relation name (should be same as initial data were)
                data: response?._embedded?.[relName] as T[] || [],
                page: response.page
            };
        }
    }

    return <KlabisTable fetchData={tableDataFetcherFactory(embeddedName)} {...tableProps}/>
}