import {useCallback, useMemo, useState} from 'react';
import * as Yup from 'yup';
import type {HalFormsTemplate, HalResponse} from '../../api';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation';
import {useToast} from '../../contexts/ToastContext';
import {useHalPageData} from '../../hooks/useHalPageData';
import {COMPOSITE_SUBFIELDS} from './compositeTypes';

function getSelfHref(resourceData: HalResponse): string | undefined {
    const selfLink = resourceData._links?.self;
    if (!selfLink) return undefined;
    if (Array.isArray(selfLink)) return selfLink[0]?.href;
    return selfLink.href;
}

function buildValidationSchema(template: HalFormsTemplate): Yup.ObjectSchema<any> {
    const shape: Record<string, Yup.AnySchema> = {};

    for (const prop of template.properties) {
        if (prop.readOnly) continue;

        let schema: Yup.AnySchema = Yup.string();

        if (prop.type === 'number') {
            schema = Yup.number();
        } else if (prop.type === 'boolean') {
            schema = Yup.boolean();
        }

        if (prop.required) {
            schema = (schema as any).required(`${prop.prompt || prop.name} je povinné pole`);
        }

        if (prop.regex) {
            schema = (schema as Yup.StringSchema).matches(
                new RegExp(prop.regex),
                `${prop.prompt || prop.name} nemá správný formát`
            );
        }

        shape[prop.name] = schema;
    }

    return Yup.object().shape(shape);
}

function buildInitialValues(
    resourceData: HalResponse,
    template: HalFormsTemplate
): Record<string, unknown> {
    const values: Record<string, unknown> = {};

    for (const prop of template.properties) {
        const subfields = COMPOSITE_SUBFIELDS[prop.type];
        if (subfields) {
            const compositeData = (resourceData[prop.name] as Record<string, unknown>) ?? {};
            const nested: Record<string, unknown> = {};
            for (const sf of subfields) {
                nested[sf] = compositeData[sf] ?? '';
            }
            values[prop.name] = nested;
        } else {
            values[prop.name] = resourceData[prop.name] ?? '';
        }
    }

    return values;
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
