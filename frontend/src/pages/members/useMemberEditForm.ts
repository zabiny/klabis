import {useCallback, useState} from 'react';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation';
import {useToast} from '../../contexts/ToastContext';
import {useHalPageData} from '../../hooks/useHalPageData';

function getSelfHref(resourceData: HalResponse): string | undefined {
    const selfLink = resourceData._links?.self;
    if (!selfLink) return undefined;
    if (Array.isArray(selfLink)) return selfLink[0]?.href;
    return selfLink.href;
}

export function useMemberEditForm(resourceData: HalResponse) {
    const [isEditing, setIsEditing] = useState(false);
    const {route} = useHalPageData();
    const {invalidateAllCaches} = useFormCacheInvalidation();
    const {addToast} = useToast();

    const template: HalFormsTemplate | null = resourceData?._templates?.default ?? null;

    const {mutateAsync, isPending, error} = useAuthorizedMutation({
        method: template?.method || 'PUT',
        onSuccess: () => {
            invalidateAllCaches().then(() => route.refetch()).catch(console.error);
            addToast('Úspěšně uloženo', 'success');
            setIsEditing(false);
        },
    });

    const handleSubmit = useCallback(
        async (values: Record<string, unknown>) => {
            const url = template?.target || getSelfHref(resourceData) || ('/api' + route.pathname);
            const editableFieldNames = new Set(template?.properties.map(p => p.name) ?? []);
            const payload = Object.fromEntries(
                Object.entries(values).filter(([key]) => editableFieldNames.has(key))
            );
            await mutateAsync({url, data: payload});
        },
        [template, resourceData, route.pathname, mutateAsync]
    );

    const startEditing = useCallback(() => setIsEditing(true), []);
    const cancelEditing = useCallback(() => setIsEditing(false), []);

    return {
        isEditing,
        startEditing,
        cancelEditing,
        template,
        handleSubmit,
        isSubmitting: isPending,
        submitError: error,
        hasTemplate: template !== null,
    };
}
