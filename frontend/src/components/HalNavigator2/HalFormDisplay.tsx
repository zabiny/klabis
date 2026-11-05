import {type ReactElement, type ReactNode, useContext, useMemo} from 'react';
import type * as Yup from 'yup';
import type {HalFormsTemplate} from '../../api';
import {ErrorDisplay, Spinner} from '../UI';
import {type HalFormFieldFactory, HalFormsForm, type RenderFormCallback} from './halforms';
import {selfHref} from '../../api/hateoas.ts';
import {UI_MESSAGES} from '../../constants/messages.ts';
import {klabisFieldsFactory} from '../KlabisFieldsFactory.tsx';
import {useHalFormData} from '../../hooks/useHalFormData.ts';
import {useAuthorizedMutation} from '../../hooks/useAuthorizedFetch.ts';
import {containerStyles} from '../../theme/designTokens';
import {useToast} from '../../contexts/toastContext';
import {useNavigate} from 'react-router-dom';
import {toRoutePath} from '../../api/apiPath';
import {withTemplateFieldPrompts} from '../../localization';
import {HalRouteContext} from '../../contexts/halRouteContext';

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
    /**
     * URL of the resource being acted on; wins over the self link of resourceData and the current route.
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
    /** Adds rules the HAL-FORMS template cannot express (e.g. cross-field requirements). Must be referentially stable. */
    refineValidationSchema?: (schema: Yup.ObjectSchema<Record<string, unknown>>) => Yup.ObjectSchema<Record<string, unknown>>;
}


/**
 * Displays a HAL Forms form with error handling and submission controls.
 * Pure form component — no modal wrapper. Wrap in Modal for modal presentation.
 */
export const HalFormDisplay = ({
                                   template: rawTemplate,
                                   templateName,
                                   resourceData,
                                   onClose,
                                   onSubmitSuccess,
                                   onSubmitError,
                                   customLayout,
                                   postprocessPayload,
                                   successMessage,
                                   fieldsFactory,
                                   submitButtonLabel,
                                   submitIcon,
                                   resourceUrl,
                                   navigateOnSuccess = true,
                                   refineValidationSchema,
                               }: HalFormDisplayProps): ReactElement => {
    const template = useMemo(() => withTemplateFieldPrompts(rawTemplate, templateName), [rawTemplate, templateName]);

    const effectiveFieldsFactory = fieldsFactory ?? klabisFieldsFactory;
    const {addToast} = useToast();
    const navigate = useNavigate();

    const {mutate: submitForm, isPending: isSubmitting, error: rawError} = useAuthorizedMutation({
        method: template.method || 'POST',
    });

    const fieldPrompts = useMemo(
        () => Object.fromEntries(template.properties.map(p => [p.name, p.prompt ?? p.name])),
        [template]
    );

    const routePathname = useContext(HalRouteContext)?.pathname;
    const currentResourceUrl = resourceUrl || selfHref(resourceData) || routePathname || '';

    const {formData, isLoadingTargetData, targetFetchError, refetchTargetData} = useHalFormData(
        template,
        resourceData,
        currentResourceUrl
    );

    const handleSubmit = async (data: Record<string, unknown>) => {
        const processed = postprocessPayload ? postprocessPayload(data, template) : data;
        const url = template.target || currentResourceUrl;
        submitForm({url, data: processed}, {
            onSuccess: async ({data: responseData, location}) => {
                const toastMessage = successMessage ?? (template.title ? `${template.title} — úspěšně uloženo` : 'Úspěšně uloženo');
                addToast(toastMessage, 'success');
                onSubmitSuccess?.(responseData);
                const willNavigate = navigateOnSuccess && template.method?.toUpperCase() === 'POST' && location != null;
                if (willNavigate) {
                    navigate(toRoutePath(location));
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

            {rawError && (
                <ErrorDisplay
                    error={rawError}
                    fieldPrompts={fieldPrompts}
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
                    serverError={rawError}
                    submitButtonLabel={submitButtonLabel}
                    submitIcon={submitIcon}
                    refineValidationSchema={refineValidationSchema}
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
