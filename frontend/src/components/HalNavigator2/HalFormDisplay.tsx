import {type ReactElement, type ReactNode} from 'react';
import type {HalFormsTemplate} from '../../api';
import {useHalPageData} from '../../hooks/useHalPageData';
import {ErrorDisplay, Spinner} from '../UI';
import {HalFormsForm, type HalFormFieldFactory, type RenderFormCallback} from './halforms';
import {isFormValidationError, toFormValidationError} from '../../api/hateoas.ts';
import {UI_MESSAGES} from '../../constants/messages.ts';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';
import {useHalFormData} from '../../hooks/useHalFormData.ts';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch.ts';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation.ts';
import {containerStyles} from '../../theme/designTokens';
import {useToast} from '../../contexts/ToastContext';

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
    /** Optional callback when submission succeeds — receives response data */
    onSubmitSuccess?: (responseData?: unknown) => void;
    /** Optional custom form layout - children or render callback */
    customLayout?: ReactNode | RenderFormCallback;
    /** Optional transform applied to payload before API submission */
    postprocessPayload?: (payload: Record<string, unknown>, template: HalFormsTemplate) => Record<string, unknown>;
    /** Optional custom toast message shown on successful submission */
    successMessage?: string;
    /** Optional custom field factory (defaults to klabisFieldsFactory) */
    fieldsFactory?: HalFormFieldFactory;
    /** Optional label for the submit button (defaults to "Odeslat") */
    submitButtonLabel?: string;
    /** Optional icon for the submit button */
    submitIcon?: ReactNode;
}


/**
 * Displays a HAL Forms form with error handling and submission controls.
 * Pure form component — no modal wrapper. Wrap in Modal for modal presentation.
 */
export const HalFormDisplay = ({
                                   template,
                                   resourceData,
                                   pathname,
                                   onClose,
                                   onSubmitSuccess,
                                   customLayout,
                                   postprocessPayload,
                                   successMessage,
                                   fieldsFactory,
                                   submitButtonLabel,
                                   submitIcon,
                               }: HalFormDisplayProps): ReactElement => {
    const {route} = useHalPageData();
    const {invalidateAllCaches} = useFormCacheInvalidation();
    const {addToast} = useToast();

    const {mutate: submitForm, isPending: isSubmitting, error: rawError} = useAuthorizedMutation({
        method: template.method || 'POST',
    });

    const submitError = rawError ? toFormValidationError(rawError) : null;

    const {formData, isLoadingTargetData, targetFetchError, refetchTargetData} = useHalFormData(
        template,
        resourceData,
        pathname
    );

    const handleSubmit = async (data: Record<string, unknown>) => {
        const processed = postprocessPayload ? postprocessPayload(data, template) : data;
        const url = template.target || '/api' + pathname;
        submitForm({url, data: processed}, {
            onSuccess: async (responseData: unknown) => {
                await invalidateAllCaches();
                await route.refetch();
                const toastMessage = successMessage ?? (template.title ? `${template.title} — úspěšně uloženo` : 'Úspěšně uloženo');
                addToast(toastMessage, 'success');
                onSubmitSuccess?.(responseData);
                onClose();
            },
        });
    };

    return (
        <div data-testid="hal-forms-display">
            {isLoadingTargetData && (
                <div className={containerStyles.loadingContainer}>
                    <Spinner size="sm"/>
                    <span>{UI_MESSAGES.LOADING_FORM_DATA}</span>
                </div>
            )}

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

            {submitError && (
                <ErrorDisplay
                    error={submitError}
                />
            )}

            {!isLoadingTargetData && !targetFetchError && formData && (
                <HalFormsForm
                    data={formData}
                    template={template}
                    onSubmit={handleSubmit}
                    onCancel={onClose}
                    isSubmitting={isSubmitting}
                    fieldsFactory={fieldsFactory ?? klabisFieldsFactory}
                    serverValidationErrors={submitError && isFormValidationError(submitError) ? submitError.validationErrors : undefined}
                    submitButtonLabel={submitButtonLabel}
                    submitIcon={submitIcon}
                    {...(customLayout
                        ? (typeof customLayout === 'function'
                            ? {renderForm: customLayout as RenderFormCallback}
                            : {children: customLayout as ReactNode})
                        : {})}
                />
            )}
        </div>
    );
};
