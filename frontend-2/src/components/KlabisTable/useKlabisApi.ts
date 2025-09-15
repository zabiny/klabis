import {type ApiParams, type PaginatedResponse} from './types';
import {useApiQuery} from "../../hooks/useApi.ts";

export const useKlabisApi = <T>(
    endpoint: string,
    params: ApiParams,
    queryKey: string = 'klabis-table'
) => {
    return useApiQuery<PaginatedResponse<T>>(
        [queryKey, endpoint, JSON.stringify(params)],
        endpoint, {
            keepPreviousData: true,
        });
};
