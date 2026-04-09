import {createContext, type ReactElement, type ReactNode, useContext, useEffect, useState} from 'react';
import {useLocation, useNavigate, useSearchParams as useSearchParamsRouter} from 'react-router-dom';
import type {RenderFormCallback} from '../components/HalNavigator2/halforms';

/**
 * Represents a request to display a form
 */
export interface HalFormRequest {
    /** Name of the HAL Forms template to display */
    templateName: string;

    /** If true, form should be displayed in modal. If false, inline */
    modal: boolean;

    /** Optional custom form layout */
    customLayout?: ReactNode | RenderFormCallback;

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
 * Safe hook to use useSearchParams that gracefully handles being outside Router context
 */
function useSafeSearchParams() {
    try {
        return useSearchParamsRouter();
    } catch {
        return [new URLSearchParams(), () => {
        }] as const;
    }
}

function useSafeLocation() {
    try {
        return useLocation();
    } catch {
        return {pathname: '/', search: '', hash: '', state: null, key: 'default'};
    }
}

function useSafeNavigate() {
    try {
        return useNavigate();
    } catch {
        return (() => {}) as ReturnType<typeof useNavigate>;
    }
}

/**
 * Provider component for HalFormContext
 * Must wrap components that use useHalForm hook
 *
 * Automatically detects and handles:
 * - Modal form requests via requestForm()
 * - Inline form requests via URL query parameter (?form=templateName)
 */
export function HalFormProvider({children}: { children: ReactNode }): ReactElement {
    const [currentFormRequest, setCurrentFormRequest] = useState<HalFormRequest | null>(null);
    const [searchParams] = useSafeSearchParams();
    const {pathname} = useSafeLocation();
    const navigate = useSafeNavigate();

    // Detect inline form requests from URL query parameter
    useEffect(() => {
        const inlineTemplateName = searchParams.get('form');
        if (inlineTemplateName) {
            setCurrentFormRequest({
                templateName: inlineTemplateName,
                modal: false,
            });
        } else {
            // Only clear form if it was set by URL (modal: false)
            // Preserve modal forms set via requestForm()
            setCurrentFormRequest(prev =>
                prev?.modal === false ? null : prev
            );
        }
    }, [searchParams]);

    const requestForm = (request: HalFormRequest) => {
        if (request.modal) {
            setCurrentFormRequest(request);
        } else {
            // Display form inline on current page (target URL only used for form data/submission)
            navigate(`${pathname}?form=${request.templateName}`);
        }
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
