/**
 * Component for displaying HAL Forms in both modal and inline contexts
 * Handles form data fetching, submission, and error handling
 */

import {type ReactElement, useState} from 'react';
import type {HalFormsTemplate, TemplateTarget} from '../../api';
import {useHalRoute} from '../../contexts/HalRouteContext.tsx';
import {Alert, Spinner} from '../UI';
import {HalFormsForm} from '../HalFormsForm';
import {isFormValidationError, submitHalFormsData} from '../../api/hateoas.ts';
import {UI_MESSAGES} from '../../constants/messages.ts';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';
import {useHalFormData} from '../../hooks/useHalFormData.ts';

/**
 * Props for HalFormDisplay component
 */
export interface HalFormDisplayProps {
    /** HAL Forms template to display */
    template: HalFormsTemplate;
    /** Name of the template */
    templateName: string;
    /** Current resource data */
    resourceData: Record<string, unknown>;
    /** Current pathname */
    pathname: string;
    /** Callback when form should be closed */
    onClose: () => void;
    /** Optional callback when submission succeeds */
    onSubmitSuccess?: () => void;
    /** Whether to show close button (default: true) */
    showCloseButton?: boolean;
}

/**
 * Displays a HAL Forms form with error handling and submission controls
 * Can be used in both modal and inline contexts
 */
export const HalFormDisplay = ({
                                   template,
                                   templateName,
                                   resourceData,
                                   pathname,
                                   onClose,
                                   onSubmitSuccess,
                                   showCloseButton = true,
                               }: HalFormDisplayProps): ReactElement => {
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

            // Close the form
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
                    {showCloseButton && (
                        <button
                            onClick={onClose}
                            className="text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-200 transition-colors"
                            data-testid="close-form-button"
                            aria-label="Close form"
                        >
                            {UI_MESSAGES.CLOSE}
                        </button>
                    )}
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
                                <button
                                    onClick={refetchTargetData}
                                    className="text-sm px-3 py-1 bg-primary text-white rounded hover:bg-primary-dark"
                                >
                                    {UI_MESSAGES.RETRY}
                                </button>
                                <button
                                    onClick={onClose}
                                    className="text-sm px-3 py-1 bg-gray-300 text-gray-900 rounded hover:bg-gray-400"
                                >
                                    {UI_MESSAGES.CANCEL}
                                </button>
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
