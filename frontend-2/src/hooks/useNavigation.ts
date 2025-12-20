import {useCallback, useMemo, useState} from "react";

export interface Navigation<T> {
    current: T;
    navigate: (resource: T) => void;
    back: () => void;
    isFirst: boolean,
    isLast: boolean,
    reset: () => void;
}

export const useNavigation = <T, >(initial: T): Navigation<T> => {
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

    const current = useMemo(() => navigation && navigation[navigation.length - 1] || initial, [navigation, initial]);

    const isFirst = navigation.length === 1;
    const isLast = true;    // doesn't keep forward (yet)

    return {current, navigate, back, isFirst, isLast, reset};
};
