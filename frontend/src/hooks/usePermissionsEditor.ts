import {useToast} from '../contexts/ToastContext';
import {useAuthorizedMutation, useAuthorizedQuery} from './useAuthorizedFetch';
import {FetchError} from '../api/authorizedFetch';
import {labels} from '../localization';
import {useFormCacheInvalidation} from './useFormCacheInvalidation';

interface PermissionsResponse {
    authorities: string[];
    _links?: {
        self?: { href: string };
    };
}

export interface UsePermissionsEditorResult {
    permissions: string[] | undefined;
    isLoading: boolean;
    save: (authorities: string[]) => void;
    isSaving: boolean;
    error: Error | null | undefined;
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

    const {data, isLoading} = useAuthorizedQuery<PermissionsResponse>(permissionsUrl ?? '', {
        enabled: (options?.enabled ?? true) && !!permissionsUrl,
        staleTime: 60_000,
    });

    const putUrl = data?._links?.self?.href ?? permissionsUrl ?? '';

    const {mutate, isPending, error: mutationError} = useAuthorizedMutation({
        method: 'PUT',
    });

    const save = (authorities: string[]) => {
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
        isLoading,
        save,
        isSaving: isPending,
        error: mutationError,
    };
}
