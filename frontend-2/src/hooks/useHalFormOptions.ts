import {useQuery} from '@tanstack/react-query';
import {authorizedFetch} from '../api/authorizedFetch';
import {getApiBaseUrl} from '../utils/getApiBaseUrl';
import type {SelectOption} from '../components/FormFields';
import type {HalFormsOption, HalFormsOptionType} from '../api';

interface UseHalFormOptionsResult {
    options: SelectOption[];
    isLoading: boolean;
    error: Error | null;
}

/**
 * Hook to fetch and manage form options from HAL+Forms definitions.
 *
 * Handles both inline options (returned immediately) and link-based options
 * (fetched from API). Uses React Query for caching and deduplication.
 *
 * @param optionDef - HAL+Forms option definition with either inline or link
 * @returns Object with options array, loading state, and error state
 *
 * @example
 * // Inline options
 * const {options, isLoading} = useHalFormOptions({
 *   inline: ['Option 1', 'Option 2']
 * });
 *
 * @example
 * // Link options
 * const {options, isLoading} = useHalFormOptions({
 *   link: {href: '/api/form-options'}
 * });
 */
export function useHalFormOptions(optionDef: HalFormsOption | undefined): UseHalFormOptionsResult {
    // Handle inline options - no fetching needed
    if (optionDef?.inline) {
        return {
            options: convertToSelectOptions(optionDef.inline),
            isLoading: false,
            error: null,
        };
    }

    // Handle link-based options - use React Query for fetching
    const linkHref = optionDef?.link?.href;
    const {data, isLoading, error} = useQuery({
        queryKey: ['hal-form-options', linkHref],
        queryFn: async () => {
            if (!linkHref) {
                return [];
            }

            // Normalize the URL for production/development environments
            // HAL links can be full URLs or paths
            let urlToFetch = linkHref;
            const apiBaseUrl = getApiBaseUrl();

            // Extract pathname from full URL if needed
            if (linkHref.includes('://')) {
                try {
                    urlToFetch = new URL(linkHref).pathname;
                } catch {
                    // If URL parsing fails, use as-is
                }
            }

            // In development: keep /api/ prefix (Vite proxy handles it)
            // In production: strip /api/ prefix (endpoints don't have it)
            if (!apiBaseUrl && urlToFetch.startsWith('/api/')) {
                urlToFetch = urlToFetch.substring(4); // Remove /api prefix
            }

            try {
                const response = await authorizedFetch(
                    urlToFetch,
                    {
                        headers: {
                            'Accept': 'application/hal+forms,application/hal+json,application/json',
                        },
                    },
                    false
                );
                if (!response.ok) {
                    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
                }
                return await response.json();
            } catch (err) {
                throw err instanceof Error ? err : new Error(String(err));
            }
        },
        enabled: !!linkHref,
        staleTime: 5 * 60 * 1000, // 5 minutes - options rarely change
    });

    const options = data ? convertToSelectOptions(data) : [];
    const finalError = error instanceof Error ? error : null;

    return {
        options,
        isLoading,
        error: finalError,
    };
}

/**
 * Convert HAL+Forms options to SelectOption format.
 *
 * Handles various option structures:
 * - Simple strings: 'Option' → {value: 'Option', label: 'Option'}
 * - Simple numbers: 42 → {value: '42', label: '42'}
 * - Objects with value and prompt: {value: 'id', prompt: 'Display'}
 * - Nested structures: {value: {nested: 'obj'}, prompt: 'Label'}
 *
 * @param halOptions - Array of HAL+Forms options
 * @returns Array of SelectOption objects
 */
export function convertToSelectOptions(halOptions: HalFormsOptionType[]): SelectOption[] {
    if (!halOptions) return [];

    return halOptions.map((item) => {
        const value = getValue(item);
        const label = getLabel(item);
        return {value, label};
    });
}

/**
 * Type guard to check if an item is an option object with value property.
 */
function isOptionItem(item: any): item is { value: any; prompt?: string } {
    return item !== undefined && item !== null && item.value !== undefined;
}

/**
 * Type guard to check if a value is a number.
 */
function isNumber(item: any): item is number {
    return typeof item === 'number';
}

/**
 * Convert any value to a string representation for form options.
 */
function optionValueToString(value: any): string {
    if (isNumber(value)) {
        return `${value}`;
    } else {
        return String(value);
    }
}

/**
 * Extract the value from a HAL+Forms option item.
 *
 * Recursively handles nested option objects.
 */
function getValue(item: HalFormsOptionType): string {
    if (isOptionItem(item)) {
        return getValue(item.value);
    } else {
        return optionValueToString(item);
    }
}

/**
 * Extract the label from a HAL+Forms option item.
 *
 * Uses the prompt if available, otherwise uses the string representation
 * of the value. Recursively handles nested option objects.
 */
function getLabel(item: HalFormsOptionType): string {
    if (isOptionItem(item)) {
        return item.prompt || getLabel(item.value);
    } else if (isNumber(item)) {
        return `${item}`;
    } else {
        return String(item);
    }
}
