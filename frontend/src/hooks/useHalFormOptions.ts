import type {SelectOption} from '../components/UI/forms';
import type {HalFormsOption, HalFormsOptionType} from '../api';
import {useAuthorizedQuery} from "./useAuthorizedFetch.ts";
import {normalizeKlabisApiPath} from "../utils/halFormsUtils.ts";
import {getPermissionInfo, labels} from '../localization';

/**
 * Minimal HAL-FORMS property context needed to choose the translation group for
 * option values. The backend sends enum options as values only and pins the property
 * `type` via `x-hal-input-type` in the API spec, so the type alone selects the group.
 */
export interface EnumOptionContext {
    type?: string;
}

const AUTHORITY_GROUP = 'authority';

const ENUM_GROUP_BY_TYPE: Record<string, string> = {
    Gender: 'gender',
    DeactivationReason: 'deactivationReason',
    SyncResolution: 'resolution',
    Authority: AUTHORITY_GROUP,
};

function resolveEnumGroup(prop?: EnumOptionContext): string | undefined {
    return prop?.type ? ENUM_GROUP_BY_TYPE[prop.type] : undefined;
}

function lookupTranslation(group: string, value: string): string | undefined {
    if (group === AUTHORITY_GROUP) {
        return getPermissionInfo(value)?.label;
    }
    const enumGroups = labels.enums as Record<string, Record<string, string> | undefined>;
    return enumGroups[group]?.[value];
}

/**
 * Replaces option labels with the localised enum label when the property is a known
 * enum and a translation exists for that value. Unmapped properties and untranslated
 * values keep the label produced by {@link convertToSelectOptions} (the server-provided
 * prompt, or the raw value when no prompt was sent).
 */
export function localizeEnumOptions(options: SelectOption[], prop?: EnumOptionContext): SelectOption[] {
    const group = resolveEnumGroup(prop);
    if (!group) return options;
    return options.map(option => {
        const translation = lookupTranslation(group, String(option.value));
        return translation !== undefined ? {...option, label: translation} : option;
    });
}

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
export function useHalFormOptions(
    optionDef: HalFormsOption | undefined,
    prop?: EnumOptionContext
): UseHalFormOptionsResult {
    const optionsHref = (optionDef?.link?.href && normalizeKlabisApiPath(optionDef?.link?.href)) ?? '';

    const linkOptions = useAuthorizedQuery(optionsHref, {
        enabled: !!optionsHref,
        staleTime: 5 * 60 * 1000, // 5 minutes - options rarely change
        select: (data) => data ? convertToSelectOptions(data as HalFormsOptionType[]) : []
    })

    // Handle inline options - no fetching needed
    if (optionDef?.inline) {
        return {
            options: localizeEnumOptions(convertToSelectOptions(optionDef.inline), prop),
            isLoading: false,
            error: null,
        };
    }

    return {
        options: localizeEnumOptions(linkOptions.data ?? [], prop),
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
function isOptionItem(item: unknown): item is { value: HalFormsOptionType; prompt?: string } {
    return item !== undefined && item !== null && typeof item === 'object' && 'value' in item;
}

/**
 * Type guard to check if a value is a number.
 */
function isNumber(item: unknown): item is number {
    return typeof item === 'number';
}

/**
 * Convert any value to a string representation for form options.
 */
function optionValueToString(value: HalFormsOptionType): string {
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
