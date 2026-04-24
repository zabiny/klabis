import { useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';

export function useDefaultSearchParam(
    paramName: string,
    defaultValue: string,
    shouldApply?: () => boolean,
): void {
    const [searchParams, setSearchParams] = useSearchParams();
    const appliedRef = useRef(false);

    useEffect(() => {
        if (appliedRef.current) return;
        appliedRef.current = true;

        if (searchParams.has(paramName)) return;
        if (shouldApply && !shouldApply()) return;

        setSearchParams(
            (prev) => {
                const next = new URLSearchParams(prev);
                next.set(paramName, defaultValue);
                return next;
            },
            { replace: true },
        );
    }, []); // eslint-disable-line react-hooks/exhaustive-deps
}
