import { useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';

export function useDefaultSearchParam(
    paramName: string,
    defaultValue: string,
    shouldApply?: () => boolean,
): void {
    const [searchParams, setSearchParams] = useSearchParams();
    const present = searchParams.has(paramName);

    useEffect(() => {
        if (present) return;
        if (shouldApply && !shouldApply()) return;

        setSearchParams(
            (prev) => {
                const next = new URLSearchParams(prev);
                next.set(paramName, defaultValue);
                return next;
            },
            { replace: true },
        );
    }, [present]); // eslint-disable-line react-hooks/exhaustive-deps
}
