/**
 * Shared value-submission helpers for multi-value HAL-FORMS widgets (checkbox group,
 * multi-select). Extracted so both widgets agree on how a raw array value round-trips
 * through the same property metadata (number-typed fields submit numbers, HAL-embedded
 * objects like {memberId, _links} normalize down to their scalar id, etc).
 */

/**
 * Converts a string option value to the correct submission type based on the HAL property type.
 *
 * For number-typed properties (e.g. a Set<Integer> field on backend),
 * values must be submitted as numbers — Jackson cannot deserialize string "1" into Integer.
 * For all other types (text, UUID-based member/trainer ids), string submission is correct.
 */
export function toSubmitValue(value: string, propType: string): string | number {
    if (propType === 'number') {
        return Number(value);
    }
    return value;
}

/**
 * Checks whether the array contains elements that need object/number → string normalization for UI display.
 * Pure number[] where the prop type is 'number' does NOT need normalization — they are already the correct
 * submit type and normalizeArrayValue handles string conversion for UI matching on the fly.
 */
export function needsNormalization(arr: unknown[], propType: string): boolean {
    return arr.some((item) => {
        if (item !== null && typeof item === 'object') return true;
        // number items in a non-number-typed field need string normalization
        if (typeof item === 'number' && propType !== 'number') return true;
        return false;
    });
}
