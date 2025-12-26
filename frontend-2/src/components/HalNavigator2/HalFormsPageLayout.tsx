/**
 * Layout wrapper for pages that want to display HAL Forms
 * Automatically handles query parameter detection and inline form display
 *
 * Usage:
 * <HalFormsPageLayout>
 *     <h1>My Page</h1>
 *     <p>Custom content...</p>
 * </HalFormsPageLayout>
 */

import {type ReactElement, type ReactNode} from 'react';
import {useSearchParams} from 'react-router-dom';
import {useHalRoute} from '../../contexts/HalRouteContext.tsx';
import {HalFormDisplay} from './HalFormDisplay.tsx';

interface HalFormsPageLayoutProps {
    children: ReactNode;
}

/**
 * Wrapper component for custom pages that need to display HAL Forms
 *
 * Handles:
 * 1. Query parameter detection (?form=templateName)
 * 2. Inline form display when a form is selected or display children when form is NOT selected
 *
 */
export function HalFormsPageLayout({children}: HalFormsPageLayoutProps): ReactElement {
    const {resourceData, pathname} = useHalRoute();
    const [searchParams, setSearchParams] = useSearchParams();

    // Detect if a form is open from query parameter
    const templateName = searchParams.get('form');
    const template = templateName ? resourceData?._templates?.[templateName] : null;

    return (
        <div className="space-y-6">
            {/* Inline form display when a form is selected */}
            {template && resourceData ? (
                <div className="border rounded-lg p-6">
                    <HalFormDisplay
                        template={template}
                        templateName={templateName!}
                        resourceData={resourceData}
                        pathname={pathname}
                        onClose={() => setSearchParams({})}
                        onSubmitSuccess={() => setSearchParams({})}
                    />
                </div>
            ) : children}
        </div>
    );
}
