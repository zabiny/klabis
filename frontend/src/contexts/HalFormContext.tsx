import {type ReactNode, useEffect, useState} from 'react';
import {useLocation} from 'react-router-dom';
import {HalFormContext, type HalFormRequest} from './halFormContext';

/**
 * Provider component for HalFormContext
 * Must wrap components that use useHalForm hook
 */
export function HalFormProvider({children}: { children: ReactNode }) {
    const {pathname} = useLocation();
    const [stored, setStored] = useState<{request: HalFormRequest; pathname: string} | null>(null);

    // A request is bound to the route it was made on, so a page's mount effect that requests a form is not
    // wiped by the route-change cleanup running in the same commit.
    const currentFormRequest = stored?.pathname === pathname ? stored.request : null;

    // Leaving and coming back should show page content, not a stale form.
    useEffect(() => {
        if (stored && stored.pathname !== pathname) setStored(null);
    }, [stored, pathname]);

    const requestForm = (request: HalFormRequest) => {
        setStored({request, pathname});
    };

    const closeForm = () => {
        setStored(null);
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
