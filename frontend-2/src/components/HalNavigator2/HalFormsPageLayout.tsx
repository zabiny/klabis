/**
 * Layout wrapper for pages that want to display HAL Forms
 * Automatically handles:
 * - Query parameter detection (?form=templateName) for inline forms
 * - Context-based modal form requests
 *
 * Usage:
 * <HalFormsPageLayout>
 *     <h1>My Page</h1>
 *     <p>Custom content...</p>
 * </HalFormsPageLayout>
 *
 * Modal forms are requested via useHalForm() context hook
 */

import {type ReactElement, type ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';
import {useHalRoute} from '../../contexts/HalRouteContext.tsx';
import {useHalForm} from '../../contexts/HalFormContext.tsx';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import {ModalOverlay} from '../UI';
import type {RenderFormCallback} from '../HalFormsForm';

interface HalFormsPageLayoutProps {
    children: ReactNode;
    /** Optional custom layouts per template name */
    customLayouts?: Record<string, ReactNode | RenderFormCallback>;
}

/**
 * Wrapper component for custom pages that need to display HAL Forms
 *
 * Handles:
 * 1. Query parameter detection (?form=templateName) for inline forms
 * 2. Context-based modal form requests
 * 3. Displays inline forms on the current page or modal overlays
 *
 */
export function HalFormsPageLayout({children, customLayouts}: HalFormsPageLayoutProps): ReactElement {
    const {resourceData, pathname} = useHalRoute();
    const [searchParams, setSearchParams] = useSearchParams();
    const {currentFormRequest, closeForm} = useHalForm();

    // Detect if a modal form is requested via context
    const modalRequest = currentFormRequest;
    const modalTemplate = modalRequest ? resourceData?._templates?.[modalRequest.templateName] : null;

    // Detect if an inline form is requested via URL
    const inlineTemplateName = searchParams.get('form');
    const inlineTemplate = inlineTemplateName ? resourceData?._templates?.[inlineTemplateName] : null;

    return (
        <>
            {/* Modal form display when context has a request */}
            {modalRequest && modalTemplate && resourceData && (
                <ModalOverlay isOpen={true} onClose={closeForm}>
                    <HalFormDisplay
                        template={modalTemplate}
                        templateName={modalRequest.templateName}
                        resourceData={resourceData}
                        pathname={pathname}
                        onClose={closeForm}
                        showCloseButton={true}
                        customLayout={modalRequest.customLayout}
                    />
                </ModalOverlay>
            )}

            {/* Page content - shows inline form or children */}
            <div className="space-y-6">
                {/* Inline form display when a form is selected via URL and no modal is open */}
                {inlineTemplate && resourceData && !modalRequest ? (
                    <div className="border rounded-lg p-6">
                        <HalFormDisplay
                            template={inlineTemplate}
                            templateName={inlineTemplateName!}
                            resourceData={resourceData}
                            pathname={pathname}
                            onClose={() => setSearchParams({})}
                            onSubmitSuccess={() => setSearchParams({})}
                            customLayout={customLayouts?.[inlineTemplateName!]}
                        />
                    </div>
                ) : !modalRequest ? children : null}
            </div>
        </>
    );
}
