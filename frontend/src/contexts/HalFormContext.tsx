import {createContext, type ReactNode, useContext, useEffect, useState} from 'react';
import {useLocation} from 'react-router-dom';
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
interface HalFormContextValue {
    /** Currently requested form (null if no form is requested) */
    currentFormRequest: HalFormRequest | null;

    /** Request to display a form */
    displayHalForm: (request: HalFormRequest) => void;

    /** Close the currently displayed form */
    closeForm: () => void;
}

/**
 * Context for managing form display requests
 * Used to communicate between HalFormButton and HalFormsPageLayout
 */
const HalFormContext = createContext<HalFormContextValue | undefined>(undefined);

/**
 * Provider component for HalFormContext
 * Must wrap components that use useHalForm hook
 */
export function HalFormProvider({children}: { children: ReactNode }) {
    const [currentFormRequest, setCurrentFormRequest] = useState<HalFormRequest | null>(null);
    const {pathname} = useLocation();

    // Reset open form when route changes — leaving and coming back should show page content, not a stale form.
    useEffect(() => {
        setCurrentFormRequest(null);
    }, [pathname]);

    const requestForm = (request: HalFormRequest) => {
        setCurrentFormRequest(request);
    };

    const closeForm = () => {
        setCurrentFormRequest(null);
    };

    const value: HalFormContextValue = {
        currentFormRequest,
        displayHalForm: requestForm,
        closeForm,
    };

    return (
        <HalFormContext.Provider value={value}>
            {children}
        </HalFormContext.Provider>
    );
}

/**
 * Hook to use HalFormContext
 * Must be called within a component wrapped by HalFormProvider
 * @throws {Error} if used outside HalFormProvider
 */
export function useHalForm(): HalFormContextValue {
    const context = useContext(HalFormContext);

    if (context === undefined) {
        throw new Error('useHalForm must be used within a component wrapped by HalFormProvider');
    }

    return context;
}
