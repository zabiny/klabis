/**
 * Layout wrapper for pages that want to display inline HAL Forms.
 * Inline form requests (HalFormButton modal={false}) replace the page content with HalFormPanel.
 * Modal forms are rendered by the button itself through useTemplateDialog.
 */

import {type ReactElement, type ReactNode} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useHalForm} from '../../contexts/halFormContext.ts';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import {HalFormPanel} from './HalFormPanel.tsx';
import type {HalFormsTemplate} from '../../api';
import {normalizeKlabisApiPath} from '../../utils/halFormsUtils.ts';

interface HalFormsPageLayoutProps {
    children: ReactNode;
}

export function HalFormsPageLayout({children}: HalFormsPageLayoutProps): ReactElement {
    const {resourceData: pageResource, route} = useHalPageData();
    const {currentFormRequest, closeForm} = useHalForm();

    const pageTemplates = pageResource?._templates as Record<string, HalFormsTemplate> | undefined;
    const template = currentFormRequest ? pageTemplates?.[currentFormRequest.templateName] : undefined;

    if (!currentFormRequest || !template) {
        return <div className="space-y-6">{children}</div>;
    }

    const selfLink = pageResource?._links?.self;
    const selfHref = Array.isArray(selfLink) ? selfLink[0]?.href : selfLink?.href;
    const resourceUrl = typeof selfHref === 'string' ? selfHref : undefined;

    const formPanel = currentFormRequest.children ? (
        <HalFormPanel
            collectionUrl={resourceUrl ?? `/api${normalizeKlabisApiPath(route.pathname)}`}
            templateName={currentFormRequest.templateName}
            template={template}
            fieldsFactory={currentFormRequest.fieldsFactory}
            onSuccess={closeForm}
            onCancel={closeForm}
            navigateOnSuccess={currentFormRequest.navigateOnSuccess}
        >
            {currentFormRequest.children}
        </HalFormPanel>
    ) : (
        <HalFormDisplay
            template={template}
            templateName={currentFormRequest.templateName}
            resourceData={(pageResource ?? {}) as Record<string, unknown>}
            pathname={route.pathname}
            resourceUrl={resourceUrl}
            onClose={closeForm}
            onSubmitSuccess={closeForm}
            fieldsFactory={currentFormRequest.fieldsFactory}
            navigateOnSuccess={currentFormRequest.navigateOnSuccess}
        />
    );

    const inlineTitle = currentFormRequest.dialogTitle ?? template.title;
    return (
        <div className="space-y-6">
            {inlineTitle && (
                <h1 className="text-3xl font-bold text-text-primary">{inlineTitle}</h1>
            )}
            {formPanel}
        </div>
    );
}
