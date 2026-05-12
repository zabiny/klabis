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
 */

import {type ReactElement, type ReactNode} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {useHalForm} from '../../contexts/HalFormContext.tsx';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import {HalFormPanel} from './HalFormPanel.tsx';
import {Modal} from '../UI';

interface HalFormsPageLayoutProps {
    children: ReactNode;
}

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
    const {resourceData, route} = useHalPageData();
    const {currentFormRequest, closeForm} = useHalForm();

    if (!currentFormRequest || !resourceData) {
        return <div className="space-y-6">{children}</div>;
    }

    const template = resourceData._templates?.[currentFormRequest.templateName];

    // If template doesn't exist, show children instead
    if (!template) {
        return <div className="space-y-6">{children}</div>;
    }

    const formPanel = currentFormRequest.children ? (
        <HalFormPanel
            collectionUrl={`/api${route.pathname}`}
            templateName={currentFormRequest.templateName}
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
            resourceData={resourceData}
            pathname={route.pathname}
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

    // Inline branch: form replaces page content
    return <div className="space-y-6">{formPanel}</div>;
}
