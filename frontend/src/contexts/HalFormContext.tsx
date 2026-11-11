import {type ReactNode, useEffect, useRef, useState} from 'react';
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
    // The initial mount is skipped: child effects run first and may already have requested a form.
    const previousPathname = useRef(pathname);
    useEffect(() => {
        if (previousPathname.current === pathname) return;
        previousPathname.current = pathname;
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
