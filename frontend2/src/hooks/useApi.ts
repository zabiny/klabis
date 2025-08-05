import {useMutation, UseMutationOptions, useQuery, UseQueryOptions} from '@tanstack/react-query';
import {AxiosError} from 'axios';
import {useEffect} from 'react';
import apiClient, {setAuthTokenGetter} from '../api/apiClient';
import {useAuth} from '../contexts/AuthContext2';

// Hook to set up the auth token getter
export const useApiSetup = () => {
    const {getAccessToken} = useAuth();

    useEffect(() => {
        setAuthTokenGetter(() => getAccessToken());
    }, [getAccessToken]);
};

// Generic type for API responses
export type ApiResponse<T> = {
    data: T;
};

// Hook for GET requests
export const useApiQuery = <T>(
    queryKey: string[],
    url: string,
    options?: UseQueryOptions<ApiResponse<T>, AxiosError>
) => {
    useApiSetup();

    return useQuery<ApiResponse<T>, AxiosError>(
        queryKey,
        async () => {
            const response = await apiClient.get<T>(url);
            return {data: response.data};
        },
        options
    );
};

// Hook for POST requests
export const useApiMutation = <T, R>(
    url: string,
    options?: UseMutationOptions<ApiResponse<R>, AxiosError, T>
) => {
    useApiSetup();

    return useMutation<ApiResponse<R>, AxiosError, T>(
        async (data: T) => {
            const response = await apiClient.post<R>(url, data);
            return {data: response.data};
        },
        options
    );
};

// Hook for PUT requests
export const useApiPutMutation = <T, R>(
    url: string,
    options?: UseMutationOptions<ApiResponse<R>, AxiosError, T>
) => {
    useApiSetup();

    return useMutation<ApiResponse<R>, AxiosError, T>(
        async (data: T) => {
            const response = await apiClient.put<R>(url, data);
            return {data: response.data};
        },
        options
    );
};

// Hook for DELETE requests
export const useApiDeleteMutation = <R>(
    url: string,
    options?: UseMutationOptions<ApiResponse<R>, AxiosError, void>
) => {
    useApiSetup();

    return useMutation<ApiResponse<R>, AxiosError, void>(
        async () => {
            const response = await apiClient.delete<R>(url);
            return {data: response.data};
        },
        options
    );
};