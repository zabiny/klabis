/**
 * Component for rendering a single HAL Forms template button
 * Shows a button that opens a form either in modal or inline on the current page.
 *
 * - Modal mode: opens the dialog locally through useTemplateDialog
 * - Inline mode: Requests inline form via HalFormContext; HalFormsPageLayout replaces page content
 *   with HalFormPanel, which receives the children render-props for custom layout
 *
 * Inline form rendering is delegated to HalFormsPageLayout
 */

import {type ReactElement, type ReactNode} from 'react';
import {useHalPageData} from '../../hooks/useHalPageData';
import {useHalForm} from '../../contexts/halFormContext.ts';
import {defaultDialogTitle, useTemplateDialog} from './useTemplateDialog.tsx';
import {HalFormTemplateButton} from './HalFormTemplateButton.tsx';
import type {HalFormFieldFactory} from './halforms';
import type {HalFormPanelChildren} from './HalFormPanel';
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
 * If it exists, renders a button. In modal mode the dialog is rendered by the button itself
 * (so it sees the nearest HalRouteProvider resource); inline mode requests the form via
 * HalFormContext and HalFormsPageLayout renders it with HalFormPanel.
 *
 * @example
 * // Modal mode
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
    const {resourceData} = useHalPageData();
    const {displayHalForm} = useHalForm();
    const templateDialog = useTemplateDialog(name, {
        resource: resourceData,
        title: dialogTitle,
        size: '2xl',
        navigateOnSuccess,
        fieldsFactory,
    });

    if (!resourceData?._templates?.[name]) {
        return null;
    }

    const template: HalFormsTemplate = resourceData._templates[name];

    const handleButtonClick = () => {
        if (modal) {
            templateDialog.open();
            return;
        }
        displayHalForm({
            templateName: name,
            children,
            fieldsFactory,
            dialogTitle: dialogTitle ?? defaultDialogTitle(name, template),
            navigateOnSuccess,
        });
    };

    return (
        <>
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
            {modal && templateDialog.dialog}
        </>
    );
}
