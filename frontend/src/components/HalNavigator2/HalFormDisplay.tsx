import {type ReactElement, type ReactNode, useMemo} from 'react';
import type {HalFormsTemplate} from '../../api';
import {useHalPageData} from '../../hooks/useHalPageData';
import {ErrorDisplay, Spinner} from '../UI';
import {HalFormsForm, type HalFormFieldFactory, type RenderFormCallback} from './halforms';
import {isFormValidationError, toFormValidationError} from '../../api/hateoas.ts';
import {UI_MESSAGES} from '../../constants/messages.ts';
import {klabisFieldsFactory, createMemberFilteredFactory} from '../KlabisFieldsFactory.tsx';
import {useHalFormData} from '../../hooks/useHalFormData.ts';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch.ts';
import {useFormCacheInvalidation} from '../../hooks/useFormCacheInvalidation.ts';
import {containerStyles} from '../../theme/designTokens';
import {useToast} from '../../contexts/ToastContext';
import {useNavigate} from 'react-router-dom';
import {extractNavigationPath} from '../../utils/navigationPath.ts';

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
    /** Optional callback when submission fails — receives the error, return true to suppress default error display */
    onSubmitError?: (error: unknown) => boolean | void;
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
    /** Member IDs to exclude from member-picker fields (already in group) */
    excludeMemberIds?: string[];
    /** When provided, member-picker fields show ONLY these member IDs (used for promote-to-owner) */
    includeOnlyMemberIds?: string[];
    /**
     * URL of the specific resource item being acted on.
     * Used as submission URL fallback when template.target is absent (e.g. embedded item affordances).
     * Absolute URLs are stripped to path so requests go through the Vite dev proxy.
     */
    resourceUrl?: string;
    /**
     * When false, suppresses auto-navigation after POST+Location response.
     * Useful for pages that have no detail route (e.g. CategoryPresetsPage).
     * Defaults to true for backward compatibility.
     */
    navigateOnSuccess?: boolean;
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
                                   onSubmitError,
                                   customLayout,
                                   postprocessPayload,
                                   successMessage,
                                   fieldsFactory,
                                   submitButtonLabel,
                                   submitIcon,
                                   excludeMemberIds,
                                   includeOnlyMemberIds,
                                   resourceUrl,
                                   navigateOnSuccess = true,
                               }: HalFormDisplayProps): ReactElement => {
    const {route} = useHalPageData();

    const effectiveFieldsFactory = useMemo(() => {
        if (fieldsFactory) return fieldsFactory;
        if (excludeMemberIds || includeOnlyMemberIds) {
            return createMemberFilteredFactory(excludeMemberIds, includeOnlyMemberIds);
        }
        return klabisFieldsFactory;
    }, [fieldsFactory, excludeMemberIds, includeOnlyMemberIds]);
    const {invalidateAllCaches} = useFormCacheInvalidation();
    const {addToast} = useToast();
    const navigate = useNavigate();

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
        const resolvedResourceUrl = resourceUrl
            ? (() => {
                try {
                    const parsed = new URL(resourceUrl);
                    return parsed.pathname;
                } catch {
                    return resourceUrl;
                }
            })()
            : undefined;
        const url = template.target || resolvedResourceUrl || '/api' + pathname;
        submitForm({url, data: processed}, {
            onSuccess: async ({data: responseData, location}) => {
                await invalidateAllCaches();
                await route.refetch();
                const toastMessage = successMessage ?? (template.title ? `${template.title} — úspěšně uloženo` : 'Úspěšně uloženo');
                addToast(toastMessage, 'success');
                onSubmitSuccess?.(responseData);
                const willNavigate = navigateOnSuccess && template.method?.toUpperCase() === 'POST' && location != null;
                if (willNavigate) {
                    navigate(extractNavigationPath(location));
                } else {
                    onClose();
                }
            },
            onError: (error: unknown) => {
                const suppressed = onSubmitError?.(error);
                if (suppressed) onClose();
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
                    fieldsFactory={effectiveFieldsFactory}
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
