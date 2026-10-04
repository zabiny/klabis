import {useMemo, useState} from 'react';
import type {HalFormsTemplate} from '../api';
import {enrichTemplateWithReadOnlyFields} from '../utils/halFormsUtils.ts';

interface UseInlineEditingOptions {
    initialEditing?: boolean;
    onCancel?: () => void;
}

interface UseInlineEditingResult {
    isEditing: boolean;
    enrichedTemplate: HalFormsTemplate | null;
    enrichedFieldNames: Set<string>;
    editableFieldNames: Set<string>;
    startEditing: () => void;
    cancelEditing: () => void;
    postprocessPayload: (payload: Record<string, unknown>) => Record<string, unknown>;
}

export function useInlineEditing(
    template: HalFormsTemplate | null,
    resourceData: Record<string, unknown>,
    options: UseInlineEditingOptions = {}
): UseInlineEditingResult {
    const {initialEditing = false, onCancel} = options;
    const [isEditing, setIsEditing] = useState(initialEditing);

    const enrichedTemplate = useMemo(() => {
        if (!isEditing || !template) return null;
        return enrichTemplateWithReadOnlyFields(template, resourceData);
    }, [isEditing, template, resourceData]);

    const enrichedFieldNames = useMemo(() =>
        enrichedTemplate
            ? new Set(enrichedTemplate.properties.map(p => p.name))
            : new Set<string>(),
        [enrichedTemplate]);

    const originalEditableFieldNames = useMemo(() =>
        template
            ? new Set(template.properties.filter(p => p.readOnly !== true).map(p => p.name))
            : new Set<string>(),
        [template]);

    const startEditing = () => setIsEditing(true);

    const cancelEditing = () => {
        if (onCancel) {
            onCancel();
        } else {
            setIsEditing(false);
        }
    };

    const postprocessPayload = (payload: Record<string, unknown>): Record<string, unknown> =>
        Object.fromEntries(
            Object.entries(payload).filter(([key]) => originalEditableFieldNames.has(key))
        );

    return {
        isEditing,
        enrichedTemplate,
        enrichedFieldNames,
        editableFieldNames: originalEditableFieldNames,
        startEditing,
        cancelEditing,
        postprocessPayload,
    };
}
