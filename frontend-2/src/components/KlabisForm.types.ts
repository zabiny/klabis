import {UseMutationResult, UseQueryResult} from "@tanstack/react-query";

export interface KlabisFormProperties<T> {
    formData: T;
    onSubmit: (formData: T) => Promise<void>;
}

export interface KlabisApiFormProperties {
    apiPath: string;
    form: React.ComponentType<KlabisFormProperties<any>>;
    onSuccess?: () => void;
}

export interface KlabisQueryFormProperties<T> {
    useGetData: () => UseQueryResult<T>,
    useMutateData: () => UseMutationResult,
    form: React.ComponentType<KlabisFormProperties<T>>,
    onSuccess?: () => void
}