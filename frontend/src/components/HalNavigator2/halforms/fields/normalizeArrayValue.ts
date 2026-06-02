/**
 * Extracts a scalar value from a form array element as a string.
 *
 * When a HAL resource contains a List field (e.g. trainers), the API returns
 * full embedded objects like {memberId: "uuid", _links: {...}}. The checkbox
 * group options use string values for UI comparison, so raw objects must be
 * normalized to their scalar string ID before comparison or display.
 */
export function normalizeArrayValue(item: unknown): string {
    if (typeof item === 'number') {
        return String(item);
    }
    if (typeof item === 'string') {
        return item;
    }
    if (item !== null && typeof item === 'object') {
        const obj = item as Record<string, unknown>;
        if (typeof obj.memberId === 'string') return obj.memberId;
        if (typeof obj.value === 'string') return obj.value;
        if (typeof obj.value === 'number') return String(obj.value);
    }
    return String(item);
}
