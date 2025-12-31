/**
 * Component for displaying HAL Forms in both modal and inline contexts
 * Handles form data fetching, submission, and error handling
 */

import {type ReactElement, type ReactNode} from 'react';
import type {HalFormsTemplate} from '../../api';
import {useHalRoute} from '../../contexts/HalRouteContext.tsx';
import {ErrorDisplay, Spinner} from '../UI';
import {HalFormsForm, type RenderFormCallback} from './halforms';
import {toFormValidationError} from '../../api/hateoas.ts';
import {UI_MESSAGES} from '../../constants/messages.ts';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';
import {useHalFormData} from '../../hooks/useHalFormData.ts';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch.ts';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation.ts';
import {buttonStyles, containerStyles, layoutStyles} from '../../theme/designTokens';

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
            <div className={containerStyles.formContainer}>
                <div className={layoutStyles.headerRow}>
                    <h4 className="font-semibold">{template.title || templateName}</h4>
                    {showCloseButton && (
                        <button
                            onClick={onClose}
                            className={buttonStyles.closeButton}
                            data-testid="close-form-button"
                            aria-label="Close form"
                        >
                            {UI_MESSAGES.CLOSE}
                        </button>
                    )}
                </div>

                {/* Loading state while fetching target data */}
                {isLoadingTargetData && (
                    <div className={containerStyles.loadingContainer}>
                        <Spinner size="sm"/>
                        <span>{UI_MESSAGES.LOADING_FORM_DATA}</span>
                    </div>
                )}

                {/* Error state when target fetch fails */}
                {targetFetchError && (
                    <ErrorDisplay
                        error={targetFetchError}
                        title={UI_MESSAGES.FORM_DATA_LOAD_ERROR}
                        subtitle={template.target ? `Endpoint: ${template.target}` : undefined}
                        onRetry={refetchTargetData}
                        onCancel={onClose}
                        retryText={UI_MESSAGES.RETRY}
                        cancelText={UI_MESSAGES.CANCEL}
                    />
                )}

                {/* Form submission error (different from target fetch error) */}
                {submitError && (
                    <ErrorDisplay
                        error={submitError}
                    />
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
                        {...(customLayout
                            ? (typeof customLayout === 'function'
                                ? {renderForm: customLayout as RenderFormCallback}
                                : {children: customLayout as ReactNode})
                            : {})}
                    />
                )}
            </div>
        </div>
    );
};
