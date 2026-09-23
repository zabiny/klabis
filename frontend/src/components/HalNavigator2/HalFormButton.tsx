/**
 * Component for rendering a single HAL Forms template button
 * Shows a button that opens a form either in modal or inline on the current page.
 *
 * - Modal mode: Requests form display via HalFormContext; HalFormsPageLayout renders the modal
 * - Inline mode: Requests inline form via HalFormContext; HalFormsPageLayout replaces page content
 *   with HalFormPanel, which receives the children render-props for custom layout
 *
 * Form rendering is delegated to HalFormsPageLayout
 */

import {type ReactElement, type ReactNode} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData';
import {type HalFormResourceContext, useHalForm} from '../../contexts/halFormContext.ts';
import {HalFormTemplateButton} from './HalFormTemplateButton.tsx';
import type {HalFormFieldFactory} from './halforms';
import type {HalFormPanelChildren} from './HalFormPanel';
import {getDialogTitleLabel, getTemplateLabel} from '../../localization';
import type {HalFormsTemplate} from '../../api';

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

    /**
     * Children render-props for custom form layout (inline mode).
     * Receives helpers: renderInput, renderField, renderLabel, hasField, hasType.
     *
     * @example
     * <HalFormButton name="createEvent" modal={false} fieldsFactory={eventFormFieldsFactory}>
     *   {({renderInput, hasField}) => (
     *     <div>
     *       {hasField('name') && renderInput('name')}
     *       {renderField('submit')}
     *     </div>
     *   )}
     * </HalFormButton>
     */
    children?: HalFormPanelChildren;

    /** Optional custom field factory for overriding individual field rendering */
    fieldsFactory?: HalFormFieldFactory;

    /** Optional additional CSS classes passed to the button */
    className?: string;

    /** Optional button variant — defaults to 'primary' */
    variant?: 'primary' | 'secondary' | 'danger' | 'ghost';

    /** Optional icon rendered before the label */
    icon?: ReactNode;

    /** Optional title shown in the dialog header (overrides template.title) */
    dialogTitle?: string;

    /** When false, suppresses auto-navigation after POST+Location response */
    navigateOnSuccess?: boolean;

    /** Hide the label text — use with icon for icon-only buttons */
    hideLabel?: boolean;
}

/**
 * Component to display a button for a specific HAL Forms template
 *
 * Checks if a template with the given name exists in the current resource.
 * If it exists, renders a button. When clicked, requests form display via HalFormContext.
 * HalFormsPageLayout handles rendering (modal overlay or inline via HalFormPanel).
 *
 * The button snapshots the current resource context (templates/pathname/self-link) into the
 * form request so HalFormsPageLayout can resolve the template and render the form even when
 * it sits outside the nested provider where the button was rendered (e.g. inside a sync
 * sub-resource fetched via HalSubresourceProvider).
 *
 * Note: Forms always display on the current resource page (which contains the _templates).
 * The template's target URL is only used as the submission endpoint.
 *
 * @example
 * // Modal mode (requires HalFormsPageLayout and HalFormProvider in parent tree)
 * <HalFormButton name="create" modal={true} />
 *
 * @example
 * // Inline mode with render-props children
 * <HalFormButton name="edit" modal={false}>
 *   {({renderInput, renderField}) => (
 *     <div>
 *       {renderInput('name')}
 *       {renderField('submit')}
 *     </div>
 *   )}
 * </HalFormButton>
 */
export function HalFormButton({name, modal = true, label, children, fieldsFactory, className, variant, icon, dialogTitle, navigateOnSuccess, hideLabel}: HalFormButtonProps): ReactElement | null {
    const {resourceData, route} = useHalPageData();
    const {displayHalForm} = useHalForm();

    // Check if template exists
    if (!resourceData?._templates?.[name]) {
        return null;
    }

    const template: HalFormsTemplate = resourceData._templates[name];

    const resolvedDialogTitle = dialogTitle ?? getDialogTitleLabel(name) ?? getTemplateLabel(name) ?? template.title;

    const handleButtonClick = () => {
        const resourceContext: HalFormResourceContext = {
            templates: resourceData._templates as Record<string, HalFormsTemplate>,
            resourceData: resourceData as Record<string, unknown>,
            pathname: route.pathname,
        };
        const selfLink = resourceData._links?.self;
        if (selfLink && !Array.isArray(selfLink) && typeof selfLink.href === 'string') {
            resourceContext.resourceUrl = selfLink.href;
        } else if (selfLink && Array.isArray(selfLink) && selfLink.length > 0) {
            resourceContext.resourceUrl = selfLink[0].href;
        }

        displayHalForm({
            templateName: name,
            modal: modal,
            children,
            fieldsFactory,
            dialogTitle: resolvedDialogTitle,
            navigateOnSuccess,
            resourceContext,
        });
    };

    return (
        <HalFormTemplateButton
            template={template}
            templateName={name}
            label={label}
            onClick={handleButtonClick}
            className={className}
            variant={variant}
            icon={icon}
            hideLabel={hideLabel}
        />
    );
}
