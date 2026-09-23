import {createContext, useContext} from 'react';
import type {HalFormsTemplate} from '../api';
import type {HalFormFieldFactory} from '../components/HalNavigator2/halforms';
import type {HalFormPanelChildren} from '../components/HalNavigator2/HalFormPanel';

/**
 * Represents a request to display a form
 */
export interface HalFormRequest {
    /** Name of the HAL Forms template to display */
    templateName: string;

    /** If true, form should be displayed in modal. If false, inline */
    modal: boolean;

    /** Children render-props for custom form layout (inline mode) */
    children?: HalFormPanelChildren;

    /** Optional custom field factory for overriding individual field rendering */
    fieldsFactory?: HalFormFieldFactory;

    /** Optional title override for the dialog header */
    dialogTitle?: string;

    /** When false, suppresses auto-navigation after POST+Location response */
    navigateOnSuccess?: boolean;

    /**
     * Optional resource override captured by the originating button.
     *
     * When a HalFormButton is rendered inside a nested HalRouteProvider (e.g.
     * a sync sub-resource fetched via HalSubresourceProvider), the page-level
     * HalFormsPageLayout sits outside that provider and only sees the
     * page-level resource. Without this override the layout would fail to
     * resolve the template by name and fall through to rendering the page
     * content instead of the form.
     *
     * The button snapshots the values it sees via useHalPageData() at click
     * time so the layout can render the form using the correct sub-resource
     * context. Page-level buttons omit this field.
     */
    resourceContext?: HalFormResourceContext;
}

/**
 * Snapshot of resource context captured at the moment a HalFormButton was
 * clicked. Lets HalFormsPageLayout resolve templates and prefill data from a
 * nested resource the layout cannot see directly.
 */
export interface HalFormResourceContext {
    /** Templates visible to the button at click time (e.g. sync sub-resource). */
    templates?: Record<string, HalFormsTemplate>;

    /** Resource data passed to HalFormDisplay for form prefill. */
    resourceData?: Record<string, unknown>;

    /** Pathname of the resource owning the templates, used for form prefill/submit URL fallback. */
    pathname?: string;

    /** Self-link href of the resource owning the templates, used as submission URL fallback. */
    resourceUrl?: string;
}

/**
 * Context value for form request management
 */
export interface HalFormContextValue {
    /** Currently requested form (null if no form is requested) */
    currentFormRequest: HalFormRequest | null;

    /** Request to display a form */
    displayHalForm: (request: HalFormRequest) => void;

    /** Close the currently displayed form */
    closeForm: () => void;
}

export const HalFormContext = createContext<HalFormContextValue | undefined>(undefined);

export function useHalForm(): HalFormContextValue {
    const context = useContext(HalFormContext);

    if (context === undefined) {
        throw new Error('useHalForm must be used within a component wrapped by HalFormProvider');
    }

    return context;
}
