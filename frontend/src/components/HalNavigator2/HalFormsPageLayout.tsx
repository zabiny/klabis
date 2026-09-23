/**
 * Layout wrapper for pages that want to display HAL Forms.
 * Automatically handles:
 * - Context-based modal form requests (renders Modal + HalFormDisplay)
 * - Context-based inline form requests (renders HalFormPanel with children render-props)
 *
 * Usage:
 * <HalFormsPageLayout>
 *     <h1>My Page</h1>
 *     <p>Custom content...</p>
 * </HalFormsPageLayout>
 *
 * Forms are requested via HalFormButton (which calls useHalForm().displayHalForm()).
 *
 * When the originating HalFormButton was rendered inside a nested HalRouteProvider (e.g.
 * inside a sync sub-resource fetched via HalSubresourceProvider), it captures that context
 * into HalFormRequest.resourceContext. This layout uses the override to resolve the template
 * and render the form, falling back to the page-level useHalPageData() when no override is
 * provided.
 */

import {type ReactElement, type ReactNode} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useHalForm} from '../../contexts/halFormContext.ts';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import {HalFormPanel} from './HalFormPanel.tsx';
import {Modal} from '../UI';
import type {HalFormsTemplate} from '../../api';
import {normalizeKlabisApiPath} from '../../utils/halFormsUtils.ts';

interface HalFormsPageLayoutProps {
    children: ReactNode;
}

const resolveTemplate = (
    templateName: string,
    overrideTemplates: Record<string, HalFormsTemplate> | undefined,
    pageTemplates: Record<string, HalFormsTemplate> | undefined,
): HalFormsTemplate | undefined => {
    if (overrideTemplates && overrideTemplates[templateName]) {
        return overrideTemplates[templateName];
    }
    return pageTemplates?.[templateName];
};

/**
 * Wrapper component for custom pages that need to display HAL Forms
 *
 * Automatically renders:
 * - Modal forms when requested via useHalForm() context with modal: true
 * - Inline forms when requested via useHalForm() context with modal: false
 *   (uses HalFormPanel with children render-props from the request)
 * - Children content when no form is requested
 *
 * Handles:
 * - Template validation (shows children if template doesn't exist)
 * - Form display and lifecycle
 */
export function HalFormsPageLayout({children}: HalFormsPageLayoutProps): ReactElement {
    const {resourceData: pageResource, route} = useHalPageData();
    const {currentFormRequest, closeForm} = useHalForm();

    const override = currentFormRequest?.resourceContext;
    const pageTemplates = pageResource?._templates as Record<string, HalFormsTemplate> | undefined;
    const template = currentFormRequest
        ? resolveTemplate(currentFormRequest.templateName, override?.templates, pageTemplates)
        : undefined;

    if (!currentFormRequest || !template) {
        return <div className="space-y-6">{children}</div>;
    }

    const effectiveResourceData = (override?.resourceData ?? pageResource ?? {}) as Record<string, unknown>;
    const effectivePathname = override?.pathname ?? route.pathname;
    const effectiveResourceUrl = override?.resourceUrl;

    const formPanel = currentFormRequest.children ? (
        <HalFormPanel
            collectionUrl={effectiveResourceUrl ?? `/api${normalizeKlabisApiPath(effectivePathname)}`}
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
            resourceData={effectiveResourceData}
            pathname={effectivePathname}
            resourceUrl={effectiveResourceUrl}
            onClose={closeForm}
            onSubmitSuccess={closeForm}
            fieldsFactory={currentFormRequest.fieldsFactory}
            navigateOnSuccess={currentFormRequest.navigateOnSuccess}
        />
    );

    if (currentFormRequest.modal) {
        return (
            <>
                <div className="space-y-6">{children}</div>
                <Modal
                    isOpen={true}
                    onClose={closeForm}
                    title={currentFormRequest.dialogTitle ?? template.title}
                    size="2xl"
                >
                    {formPanel}
                </Modal>
            </>
        );
    }

    // Inline branch: form replaces page content; show a heading so user keeps context
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
