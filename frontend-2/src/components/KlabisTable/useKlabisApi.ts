import {type ApiParams, type PaginatedResponse} from './types';
import {useApiQuery} from "../../hooks/useApi.ts";

export const useKlabisApi = <T>(
    endpoint: string,
    params: ApiParams,
    queryKey: string = 'klabis-table'
) => {
    const {sort, ...others} = params;

    const queryParams = new URLSearchParams();
    sort.forEach(s => {
        queryParams.append('sort', s)
    })

    if (others) {
        Object.entries(others).forEach(([key, value]) => {
            queryParams.append(key, value.toString());
        });
    }

    return useApiQuery<PaginatedResponse<T>>(
        [queryKey, endpoint, JSON.stringify(params)],
        endpoint,
        queryParams,
        {
            keepPreviousData: true,
        });
};
