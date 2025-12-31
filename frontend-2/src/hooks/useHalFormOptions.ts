import type {SelectOption} from '../components/UI/forms';
import type {HalFormsOption, HalFormsOptionType} from '../api';
import {useAuthorizedQuery} from "./useAuthorizedFetch.ts";
import {normalizeKlabisApiPath} from "../utils/halFormsUtils.ts";

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
    const optionsHref = (optionDef?.link?.href && normalizeKlabisApiPath(optionDef?.link?.href)) ?? '';

    const linkOptions = useAuthorizedQuery(optionsHref, {
        enabled: !!optionsHref,
        staleTime: 5 * 60 * 1000, // 5 minutes - options rarely change
        select: (data) => data ? convertToSelectOptions(data as HalFormsOptionType[]) : []
    })

    // Handle inline options - no fetching needed
    if (optionDef?.inline) {
        return {
            options: convertToSelectOptions(optionDef.inline),
            isLoading: false,
            error: null,
        };
    }

    return {
        options: linkOptions.data ?? [],
        isLoading: linkOptions.isLoading,
        error: linkOptions.error
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
