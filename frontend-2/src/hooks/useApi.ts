import {useMutation, type UseMutationOptions, useQuery, type UseQueryOptions} from '@tanstack/react-query';
import {AxiosError, type AxiosInstance} from 'axios';
import {useEffect} from 'react';
import apiClient, {setAuthTokenGetter} from '../api/apiClient';
import {useAuth} from '../contexts/AuthContext2';

export const useKlabisApiClient = (): AxiosInstance => {
    const {getAccessToken} = useAuth();

    useEffect(() => {
        setAuthTokenGetter(() => getAccessToken());
    }, [getAccessToken]);

    return apiClient;
}

// Hook for GET requests
export const useApiQuery = <T>(
    queryKey: string[],
    url: string,
    apiParams?: string | URLSearchParams,
    options?: UseQueryOptions<T, AxiosError>
) => {
    const klabisAxios = useKlabisApiClient();

    return useQuery<T, AxiosError>({
        queryKey,
        queryFn: async () => {
            const response = await klabisAxios.get<T>(url, {
                params: apiParams,
                headers: {
                    Accept: 'application/klabis+json,application/json',
                },
            });
            return response.data;
        },
        ...options
    });
};

// Hook for POST requests
export const useApiPostMutation = <T, R>(
    url: string,
    options?: UseMutationOptions<R, AxiosError, T>
) => {
    const klabisAxios = useKlabisApiClient();

    return useMutation<R, AxiosError, T>({
            mutationFn: async (data: T) => {
                const response = await klabisAxios.post<R>(url, data);
                return response.data;
            },
            ...options
        }
    );
};

// Hook for PUT requests
export const useApiPutMutation = <T, R>(
    url: string,
    options?: UseMutationOptions<R, AxiosError, T>
) => {
    const klabisAxios = useKlabisApiClient();

    return useMutation<R, AxiosError, T>({
            mutationFn: async (data: T) => {
                const response = await klabisAxios.put<R>(url, data);
                return response.data;
            },
            ...options
        }
    );
};

// Hook for DELETE requests
export const useApiDeleteMutation = <R>(
    url: string,
    options?: UseMutationOptions<R, AxiosError, void>
) => {
    const klabisAxios = useKlabisApiClient();

    return useMutation<R, AxiosError, void>({
            mutationFn: async () => {
                const response = await klabisAxios.delete<R>(url);
                return response.data;
            },
            ...options
        }
    );
};
