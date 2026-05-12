import type {ReactElement} from 'react';
import type {HalFormFieldFactory, RenderFormCallback, FormRenderHelpers} from './halforms';
import type {HalResponse} from '../../api';
import {useAuthorizedQuery} from '../../hooks/useAuthorizedFetch';
import {HalFormDisplay} from './HalFormDisplay';
import {Alert, Spinner} from '../UI';
import {labels} from '../../localization';

export interface HalFormPanelRenderHelpers extends FormRenderHelpers {
    hasField: (fieldName: string) => boolean;
    hasType: (typeName: string) => boolean;
}

export type HalFormPanelChildren = (helpers: HalFormPanelRenderHelpers) => ReactElement;

export interface HalFormPanelProps {
    collectionUrl: string;
    templateName: string;
    initialData?: Record<string, unknown>;
    pathname?: string;
    fieldsFactory?: HalFormFieldFactory;
    onSuccess?: (responseData?: unknown) => void;
    onCancel?: () => void;
    navigateOnSuccess?: boolean;
    templateMissingMessage?: string;
    successMessage?: string;
    children: HalFormPanelChildren;
}

export function HalFormPanel({
    collectionUrl, templateName, initialData = {}, pathname,
    fieldsFactory, onSuccess, onCancel, navigateOnSuccess,
    templateMissingMessage, successMessage, children,
}: HalFormPanelProps): ReactElement {
    const {data: collectionData, isLoading, error} = useAuthorizedQuery<HalResponse>(collectionUrl);

    if (isLoading) {
        return (
            <div className="flex items-center gap-2">
                <Spinner/>
                <span>{labels.ui.loading}</span>
            </div>
        );
    }
    if (error) return <Alert severity="error">{(error as Error).message}</Alert>;

    const template = collectionData?._templates?.[templateName] ?? null;
    if (!template) {
        return <Alert severity="error">{templateMissingMessage ?? `Template "${templateName}" není dostupný.`}</Alert>;
    }

    const resolvedPathname = pathname ?? collectionUrl.replace(/^\/api/, '');
    const fieldNameSet = new Set(template.properties.map(p => p.name));
    const fieldTypeSet = new Set(template.properties.map(p => p.type));

    const customLayout: RenderFormCallback = (helpers) => children({
        ...helpers,
        hasField: (name: string) => fieldNameSet.has(name),
        hasType: (typeName: string) => fieldTypeSet.has(typeName),
    });

    return (
        <HalFormDisplay
            template={template}
            templateName={templateName}
            resourceData={initialData}
            pathname={resolvedPathname}
            onClose={onCancel ?? (() => {})}
            onSubmitSuccess={onSuccess}
            fieldsFactory={fieldsFactory}
            navigateOnSuccess={navigateOnSuccess}
            successMessage={successMessage}
            customLayout={customLayout}
        />
    );
}
