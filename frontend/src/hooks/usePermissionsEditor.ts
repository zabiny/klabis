import {useToast} from '../contexts/toastContext';
import {useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';
import {FetchError} from '../api/authorizedFetch';
import {toHref} from '../api/hateoas';
import type {GetUserPermissionsResource} from '../api';
import {labels} from '../localization';
import {useFormCacheInvalidation} from './useFormCacheInvalidation';

export interface UsePermissionsEditorResult {
    permissions: string[] | undefined;
    assignableAuthorities: string[];
    isLoading: boolean;
    onSave: (authorities: string[]) => void;
    isSaving: boolean;
    error: Error | null | undefined;
}

function extractAssignableAuthorities(resource: GetUserPermissionsResource | undefined): string[] {
    const authoritiesProperty = resource?._templates?.updatePermissions?.properties
        ?.find(property => property.name === 'authorities');
    return authoritiesProperty?.options?.inline?.map(String) ?? [];
}

export interface UsePermissionsEditorOptions {
    enabled?: boolean;
    onSaved?: () => void;
}

export function resolvePermissionErrorMessage(error: Error): string {
    if (error instanceof FetchError && error.responseStatus === 409) {
        return labels.errors.removeLastPermissionsAdmin;
    }
    return labels.errors.savePermissionsFailed;
}

export function usePermissionsEditor(
    permissionsUrl: string | undefined,
    options?: UsePermissionsEditorOptions,
): UsePermissionsEditorResult {
    const {addToast} = useToast();
    const {invalidateAllCaches} = useFormCacheInvalidation();

    const {data, isLoading} = useAuthorizedQuery<GetUserPermissionsResource>(permissionsUrl ?? '', {
        enabled: (options?.enabled ?? true) && !!permissionsUrl,
        staleTime: 60_000,
    });

    const putUrl = (data?._links?.self && toHref(data._links.self)) ?? permissionsUrl ?? '';

    const {mutate, isPending, error: mutationError} = useAuthorizedMutation({
        method: 'PUT',
    });

    const onSave = (authorities: string[]) => {
        if (isPending) return;
        mutate(
            {url: putUrl, data: {authorities}},
            {
                onSuccess: async () => {
                    await invalidateAllCaches();
                    addToast(labels.ui.permissionsSaved, 'success');
                    options?.onSaved?.();
                },
            },
        );
    };

    return {
        permissions: data?.authorities,
        assignableAuthorities: extractAssignableAuthorities(data),
        isLoading,
        onSave,
        isSaving: isPending,
        error: mutationError,
    };
}
