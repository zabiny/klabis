/**
 * Component for rendering a single HAL Forms template button
 * Shows a button that opens a form either in modal or navigates to a new page
 */

import {type ReactElement, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import {useHalRoute} from '../contexts/HalRouteContext';
import {HalFormDisplay} from './HalFormDisplay';
import {HalFormTemplateButton} from './HalFormTemplateButton';

/**
 * Props for HalFormButton component
 */
export interface HalFormButtonProps {
    /** Name of the HAL Forms template to display */
    name: string;

    /** If true, opens form in modal overlay. If false, displays form inline on current page */
    modal: boolean;
}

/**
 * Component to display a button for a specific HAL Forms template
 *
 * Checks if a template with the given name exists in the current resource.
 * If it exists, renders a button. When clicked:
 * - In modal mode: Opens the form as an overlay
 * - In non-modal mode: Displays form inline on current page with query parameter
 *
 * Note: Forms always display on the current resource page (which contains the _templates).
 * The template's target URL is only used to fetch initial form values and as the submission endpoint.
 *
 * @example
 * // Modal mode
 * <HalFormButton name="create" modal={true} />
 *
 * @example
 * // Non-modal mode (displays form inline on current page with query param)
 * <HalFormButton name="edit" modal={false} />
 */
export function HalFormButton({name, modal}: HalFormButtonProps): ReactElement | null {
    const {resourceData, pathname} = useHalRoute();
    const navigate = useNavigate();
    const [isModalOpen, setIsModalOpen] = useState(false);

    // Check if template exists
    if (!resourceData?._templates?.[name]) {
        return null;
    }

    const template = resourceData._templates[name];

    const handleButtonClick = () => {
        if (modal) {
            setIsModalOpen(true);
        } else {
            // Display form inline on current page (target URL only used for form data/submission)
            navigate(`${pathname}?form=${name}`);
        }
    };

    const handleCloseModal = () => {
        setIsModalOpen(false);
    };

    return (
        <>
            <HalFormTemplateButton
                template={template}
                templateName={name}
                onClick={handleButtonClick}
            />

            {/* Render modal if in modal mode and open */}
            {modal && isModalOpen && (
                <div
                    className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50 animate-in fade-in duration-300">
                    <div
                        className="bg-surface max-w-2xl w-full max-h-[90vh] overflow-y-auto p-6 rounded-lg shadow-xl animate-in fade-in zoom-in-95 duration-300">
                        <HalFormDisplay
                            template={template}
                            templateName={name}
                            resourceData={resourceData}
                            pathname={pathname}
                            onClose={handleCloseModal}
                            showCloseButton={true}
                        />
                    </div>
                </div>
            )}
        </>
    );
}
