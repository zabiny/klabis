/**
 * Component for rendering a single HAL Forms template button
 * Shows a button that opens a form either in modal or inline on the current page
 *
 * - Modal mode: Requests form display via HalFormContext
 * - Inline mode: Navigates with query parameter (?form=templateName)
 *
 * Form rendering is delegated to HalFormsPageLayout
 */

import {type ReactElement, type ReactNode} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useHalForm} from '../../contexts/HalFormContext.tsx';
import {HalFormTemplateButton} from './HalFormTemplateButton.tsx';
import type {RenderFormCallback} from './halforms';

/**
 * Props for HalFormButton component
 */
export interface HalFormButtonProps {
    /** Name of the HAL Forms template to display */
    name: string;

    /** If true, opens form in modal overlay. If false, displays form inline on current page */
    modal?: boolean;

    /** Optional explicit button label — overrides template.title and templateName */
    label?: string;

    /** Optional custom form layout - children or render callback */
    customLayout?: ReactNode | RenderFormCallback;
}

/**
 * Component to display a button for a specific HAL Forms template
 *
 * Checks if a template with the given name exists in the current resource.
 * If it exists, renders a button. When clicked:
 * - In modal mode: Requests form display via HalFormContext (HalFormsPageLayout renders the modal)
 * - In non-modal mode: Navigates to current page with query parameter (?form=templateName)
 *   HalFormsPageLayout detects the parameter and renders the form inline
 *
 * Note: Forms always display on the current resource page (which contains the _templates).
 * The template's target URL is only used to fetch initial form values and as the submission endpoint.
 *
 * @example
 * // Modal mode (requires HalFormsPageLayout and HalFormProvider in parent tree)
 * <HalFormButton name="create" modal={true} />
 *
 * @example
 * // Inline mode (requires HalFormsPageLayout in parent tree)
 * <HalFormButton name="edit" modal={false} />
 */
export function HalFormButton({name, modal = true, label, customLayout}: HalFormButtonProps): ReactElement | null {
    const {resourceData} = useHalPageData();
    const {displayHalForm} = useHalForm();

    // Check if template exists
    if (!resourceData?._templates?.[name]) {
        return null;
    }

    const template = resourceData._templates[name];

    const handleButtonClick = () => {
        // Request form display via context (HalFormsPageLayout will render the modal)
        displayHalForm({
            templateName: name,
            modal: modal,
            customLayout
        });
    };

    return (
        <HalFormTemplateButton
            template={template}
            templateName={name}
            label={label}
            onClick={handleButtonClick}
        />
    );
}
