import { useState, useEffect } from 'react';

/**
 * Hook for persisting state in localStorage
 *
 * @param key - localStorage key
 * @param defaultValue - Default value if key doesn't exist
 * @returns Tuple of [value, setValue] similar to useState
 *
 * @example
 * const [rowsPerPage, setRowsPerPage] = usePersistedState('table-rows-per-page', 10);
 */
export function usePersistedState<T>(key: string, defaultValue: T): [T, (value: T) => void] {
    // Initialize state from localStorage or default
    const [value, setValue] = useState<T>(() => {
        try {
            const item = localStorage.getItem(key);
            return item ? JSON.parse(item) : defaultValue;
        } catch (error) {
            console.warn(`Failed to read localStorage key "${key}":`, error);
            return defaultValue;
        }
    });

    // Update localStorage when value changes
    useEffect(() => {
        try {
            localStorage.setItem(key, JSON.stringify(value));
        } catch (error) {
            console.warn(`Failed to write localStorage key "${key}":`, error);
        }
    }, [key, value]);

    return [value, setValue];
}
