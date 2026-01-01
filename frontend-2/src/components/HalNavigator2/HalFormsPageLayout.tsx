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
import {useHalPageData} from '../../hooks/useHalPageData';
import {useHalForm} from '../../contexts/HalFormContext.tsx';
import {HalFormDisplay} from './HalFormDisplay.tsx';
import {ModalOverlay} from '../UI';
import type {RenderFormCallback} from './halforms';

interface HalFormsPageLayoutProps {
    children: ReactNode;
    /** Optional custom layouts per template name */
    customLayouts?: Record<string, ReactNode | RenderFormCallback>;
}

/**
 * Wrapper component for custom pages that need to display HAL Forms
 *
 * Automatically renders:
 * - Modal forms when requested via useHalForm() context with modal: true
 * - Inline forms when requested via URL query param (?form=templateName)
 * - Children content when no form is requested
 *
 * Handles:
 * - Template validation (shows children if template doesn't exist)
 * - Form display and lifecycle
 */
export function HalFormsPageLayout({children, customLayouts}: HalFormsPageLayoutProps): ReactElement {
    const {resourceData, route} = useHalPageData();
    const [, setSearchParams] = useSearchParams();
    const {currentFormRequest, closeForm} = useHalForm();

    if (!currentFormRequest || !resourceData) {
        return <div className="space-y-6">{children}</div>;
    }

    const template = resourceData._templates?.[currentFormRequest.templateName];

    // If template doesn't exist, show children instead
    if (!template) {
        return <div className="space-y-6">{children}</div>;
    }

    // Handle closing inline forms by clearing search params
    const handleCloseForm = () => {
        if (!currentFormRequest.modal) {
            setSearchParams({});
        } else {
            closeForm();
        }
    };

    // Handle success for inline forms by clearing search params
    const handleSubmitSuccess = () => {
        if (!currentFormRequest.modal) {
            setSearchParams({});
        }
    };

    if (currentFormRequest.modal) {
        return (
            <>
                <div className="space-y-6">{children}</div>
                <ModalOverlay isOpen={true} onClose={handleCloseForm}>
                    <HalFormDisplay
                        template={template}
                        templateName={currentFormRequest.templateName}
                        resourceData={resourceData}
                        pathname={route.pathname}
                        onClose={handleCloseForm}
                        showCloseButton={true}
                        customLayout={currentFormRequest.customLayout}
                    />
                </ModalOverlay>
            </>
        );
    }

    return (
        <div className="space-y-6">
            <div className="border rounded-lg p-6">
                <HalFormDisplay
                    template={template}
                    templateName={currentFormRequest.templateName}
                    resourceData={resourceData}
                    pathname={route.pathname}
                    onClose={handleCloseForm}
                    onSubmitSuccess={handleSubmitSuccess}
                    customLayout={customLayouts?.[currentFormRequest.templateName]}
                />
            </div>
        </div>
    );
}
