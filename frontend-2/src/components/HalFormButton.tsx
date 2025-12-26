/**
 * Component for rendering a single HAL Forms template button
 * Shows a button that opens a form either in modal or navigates to a new page
 */

import {type ReactElement, useState} from 'react';
import {useNavigate} from 'react-router-dom';
import type {HalFormsTemplate, TemplateTarget} from '../api';
import {useHalRoute} from '../contexts/HalRouteContext';
import {Alert, Button, Spinner} from './UI';
import {HalFormsForm} from './HalFormsForm';
import {isFormValidationError, submitHalFormsData} from '../api/hateoas';
import {UI_MESSAGES} from '../constants/messages';
import {klabisFieldsFactory} from './KlabisFieldsFactory';
import {useHalFormData} from '../hooks/useHalFormData';
import {HalFormTemplateButton} from './HalFormTemplateButton';

/**
 * Props for HalFormButton component
 */
export interface HalFormButtonProps {
    /** Name of the HAL Forms template to display */
    name: string;

    /** If true, opens form in modal overlay. If false, navigates to new page */
    modal: boolean;
}

/**
 * Form display component for modal mode
 * Shows the selected form with error handling and submission controls
 */
interface HalFormModalProps {
    template: HalFormsTemplate;
    templateName: string;
    resourceData: Record<string, unknown>;
    pathname: string;
    onClose: () => void;
    onSubmitSuccess?: () => void;
}

const HalFormModal = ({
                          template,
                          templateName,
                          resourceData,
                          pathname,
                          onClose,
                          onSubmitSuccess,
                      }: HalFormModalProps): ReactElement => {
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [submitError, setSubmitError] = useState<Error | null>(null);
    const {refetch} = useHalRoute();

    const {formData, isLoadingTargetData, targetFetchError, refetchTargetData} = useHalFormData(
        template,
        resourceData,
        pathname
    );

    const handleSubmit = async (data: Record<string, unknown>) => {
        setIsSubmitting(true);
        setSubmitError(null);
        console.log('Submitting form data:', JSON.stringify(data));

        try {
            const submitTarget: TemplateTarget = {
                target: template.target || '/api' + pathname,
                method: template.method || 'POST'
            };

            await submitHalFormsData(submitTarget, data);

            // Refetch data after successful submission
            await refetch();

            // Call optional success callback
            onSubmitSuccess?.();

            // Close the modal
            onClose();
        } catch (error) {
            setSubmitError(error instanceof Error ? error : new Error(String(error)));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <div data-testid="hal-forms-display">
            <div className="space-y-4">
                <div className="flex items-center justify-between mb-4">
                    <h4 className="font-semibold">{template.title || templateName}</h4>
                    <Button
                        onClick={onClose}
                        variant="secondary"
                        size="sm"
                        data-testid="close-form-button"
                        aria-label="Close form"
                    >
                        {UI_MESSAGES.CLOSE}
                    </Button>
                </div>

                {/* Loading state while fetching target data */}
                {isLoadingTargetData && (
                    <div className="flex items-center gap-2 p-4 bg-surface-raised rounded">
                        <Spinner size="sm"/>
                        <span>{UI_MESSAGES.LOADING_FORM_DATA}</span>
                    </div>
                )}

                {/* Error state when target fetch fails */}
                {targetFetchError && (
                    <Alert severity="error">
                        <div className="space-y-2">
                            <p>{UI_MESSAGES.FORM_DATA_LOAD_ERROR}</p>
                            {template.target && (
                                <p className="text-sm text-text-secondary">
                                    Endpoint: {template.target}
                                </p>
                            )}
                            <p className="text-sm text-text-secondary">{targetFetchError.message}</p>
                            <div className="flex gap-2">
                                <Button size="sm" onClick={refetchTargetData}>
                                    {UI_MESSAGES.RETRY}
                                </Button>
                                <Button size="sm" variant="secondary" onClick={onClose}>
                                    {UI_MESSAGES.CANCEL}
                                </Button>
                            </div>
                        </div>
                    </Alert>
                )}

                {/* Form submission error (different from target fetch error) */}
                {submitError && (
                    <Alert severity="error">
                        <div className="space-y-1">
                            <p>{submitError.message}</p>
                            {isFormValidationError(submitError) && (
                                <ul className="list-disc list-inside text-sm">
                                    {Object.entries(submitError.validationErrors).map(([field, error]) => (
                                        <li key={field}>
                                            {field}: {error}
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    </Alert>
                )}

                {/* Only show form when data is ready and no target fetch error */}
                {!isLoadingTargetData && !targetFetchError && formData && (
                    <HalFormsForm
                        data={formData}
                        template={template}
                        onSubmit={handleSubmit}
                        onCancel={onClose}
                        isSubmitting={isSubmitting}
                        fieldsFactory={klabisFieldsFactory}
                    />
                )}
            </div>
        </div>
    );
};

/**
 * Component to display a button for a specific HAL Forms template
 *
 * Checks if a template with the given name exists in the current resource.
 * If it exists, renders a button. When clicked:
 * - In modal mode: Opens the form as an overlay
 * - In non-modal mode: Navigates to a new page
 *
 * @example
 * // Modal mode
 * <HalFormButton name="create" modal={true} />
 *
 * @example
 * // Non-modal mode (navigates to new page)
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
            // Navigate to form page
            // TODO: Implement navigation logic for non-modal mode
            // This will depend on your routing structure
            navigate(`${pathname}/form/${name}`);
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
                        <HalFormModal
                            template={template}
                            templateName={name}
                            resourceData={resourceData}
                            pathname={pathname}
                            onClose={handleCloseModal}
                        />
                    </div>
                </div>
            )}
        </>
    );
}
