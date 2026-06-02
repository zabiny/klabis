import {type ReactNode, useEffect, useState} from 'react';
import {useLocation} from 'react-router-dom';
import {HalFormContext, type HalFormRequest} from './halFormContext';

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

    const value = {
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
