import {createContext, type ReactElement, type ReactNode, useContext, useEffect, useState} from 'react';
import {useSearchParams as useSearchParamsRouter} from 'react-router-dom';
import type {RenderFormCallback} from '../components/HalFormsForm';

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
}

/**
 * Context value for form request management
 */
interface HalFormContextValue {
    /** Currently requested form (null if no form is requested) */
    currentFormRequest: HalFormRequest | null;

    /** Request to display a form */
    requestForm: (request: HalFormRequest) => void;

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
        // Not in a Router context, return empty URLSearchParams
        return [new URLSearchParams(), () => {
        }] as const;
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
        setCurrentFormRequest(request);
    };

    const closeForm = () => {
        setCurrentFormRequest(null);
    };

    const value: HalFormContextValue = {
        currentFormRequest,
        requestForm,
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
