import {createContext, useContext} from 'react';
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
