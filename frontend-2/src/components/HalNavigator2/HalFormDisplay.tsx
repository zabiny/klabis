/**
 * Component for displaying HAL Forms in both modal and inline contexts
 * Handles form data fetching, submission, and error handling
 */

import {type ReactElement, type ReactNode, useMemo} from 'react';
import type {HalFormsTemplate} from '../../api';
import {useHalRoute} from '../../contexts/HalRouteContext.tsx';
import {Alert, Spinner} from '../UI';
import {HalFormsForm, type RenderFormCallback} from '../HalFormsForm';
import {isFormValidationError, toFormValidationError} from '../../api/hateoas.ts';
import {UI_MESSAGES} from '../../constants/messages.ts';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';
import {useHalFormData} from '../../hooks/useHalFormData.ts';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch.ts';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation.ts';

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
    /** Optional custom form layout - children or render callback */
    customLayout?: ReactNode | RenderFormCallback;
}

/**
 * Builds props for HalFormsForm based on customLayout type
 */
function getHalFormsFormProps(customLayout?: ReactNode | RenderFormCallback): Record<string, ReactNode | RenderFormCallback> {
    if (!customLayout) {
        return {};
    }

    if (typeof customLayout === 'function') {
        return {renderForm: customLayout as RenderFormCallback};
    }

    return {children: customLayout as ReactNode};
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
                                   customLayout,
                               }: HalFormDisplayProps): ReactElement => {
    const {refetch} = useHalRoute();
    const {invalidateAllCaches} = useFormCacheInvalidation();
    const halFormsFormProps = useMemo(
        () => getHalFormsFormProps(customLayout),
        [customLayout]
    );

    const {mutate: submitForm, isPending: isSubmitting, error: rawError} = useAuthorizedMutation({
        method: template.method || 'POST',
        onSuccess: async () => {
            await invalidateAllCaches();
            await refetch();
            onSubmitSuccess?.();
            onClose();
        },
    });

    // Convert FetchError to FormValidationError if applicable
    const submitError = rawError ? toFormValidationError(rawError) : null;

    const {formData, isLoadingTargetData, targetFetchError, refetchTargetData} = useHalFormData(
        template,
        resourceData,
        pathname
    );

    const handleSubmit = async (data: Record<string, unknown>) => {
        const url = template.target || '/api' + pathname;
        submitForm({url, data});
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
                        {...halFormsFormProps}
                    />
                )}
            </div>
        </div>
    );
};
