import {useCallback, useMemo, useState} from "react";

export const useNavigation = <T, >(initial?: T): {
    current: T | undefined;
    navigate: (resource: T) => void;
    back: () => void;
    isFirst: boolean,
    isLast: boolean,
    reset: () => void;
} => {
    const [navigation, setNavigation] = useState<Array<T>>(initial && [initial] || []);

    const navigate = useCallback((resource: T): void => {
        setNavigation(prev => [...prev, resource]);
    }, []);

    const back = useCallback((): void => {
        setNavigation(prev => {
            if (prev.length < 2) return prev;
            // remove the last item from the navigation stack
            return prev.slice(0, -1);
        });
    }, []);

    const reset = useCallback(() => {
        setNavigation(prev => [prev[0]]);
    }, [])

    const current = useMemo(() => navigation && navigation[navigation.length - 1] || initial || undefined, [navigation, initial]);

    const isFirst = navigation.length == 1;
    const isLast = true;    // doesn't keep forward (yet)

    return {current, navigate, back, isFirst, isLast, reset};
};
