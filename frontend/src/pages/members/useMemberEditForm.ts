import {useCallback, useMemo, useState} from 'react';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation';
import {useToast} from '../../contexts/ToastContext';
import {useHalPageData} from '../../hooks/useHalPageData';
import {buildInitialValues, buildValidationSchema} from './formUtils';

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

    const {mutate, isPending, error} = useAuthorizedMutation({
        method: template?.method || 'PUT',
        onSuccess: () => {
            invalidateAllCaches().then(() => route.refetch());
            addToast('Usp\u011B\u0161n\u011B ulo\u017Eeno', 'success');
            setIsEditing(false);
        },
    });

    const initialValues = useMemo(() => {
        if (!template) return {};
        return buildInitialValues(resourceData, template);
    }, [resourceData, template]);

    const validationSchema = useMemo(() => {
        if (!template) return undefined;
        return buildValidationSchema(template);
    }, [template]);

    const handleSubmit = useCallback(
        (values: Record<string, unknown>) => {
            const url = template?.target || getSelfHref(resourceData) || ('/api' + route.pathname);
            mutate({url, data: values});
        },
        [template, route.pathname, mutate]
    );

    const startEditing = useCallback(() => setIsEditing(true), []);
    const cancelEditing = useCallback(() => setIsEditing(false), []);

    return {
        isEditing,
        startEditing,
        cancelEditing,
        template,
        initialValues,
        validationSchema,
        handleSubmit,
        isSubmitting: isPending,
        submitError: error,
        hasTemplate: template !== null,
    };
}
