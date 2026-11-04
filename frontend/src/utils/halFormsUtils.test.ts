import {shouldFetchTargetData} from './halFormsUtils';

describe('shouldFetchTargetData', () => {
    it('returns false when template target is undefined', () => {
        expect(shouldFetchTargetData(undefined, '/members/123')).toBe(false);
    });

    it('returns false when template target is empty string', () => {
        expect(shouldFetchTargetData('', '/members/123')).toBe(false);
    });

    it('returns false when target equals current pathname', () => {
        expect(shouldFetchTargetData('/members/123', '/members/123')).toBe(false);
    });

    it('returns false when target with /api prefix equals current pathname', () => {
        expect(shouldFetchTargetData('/api/members/123', '/members/123')).toBe(false);
    });

    it('returns false when target equals current pathname with /api prefix', () => {
        expect(shouldFetchTargetData('/members/123', '/api/members/123')).toBe(false);
    });

    it('returns true when target differs from current pathname', () => {
        expect(shouldFetchTargetData('/members/456', '/members/123')).toBe(true);
    });

    it('returns true when target is different resource', () => {
        expect(shouldFetchTargetData('/api/events/789', '/members/123')).toBe(true);
    });

    it('returns false when full URL target matches current pathname', () => {
        expect(shouldFetchTargetData('https://example.com/api/members/123', '/members/123')).toBe(false);
    });

    it('returns true when full URL target differs from current pathname', () => {
        expect(shouldFetchTargetData('https://example.com/api/events/789', '/members/123')).toBe(true);
    });

    it('handles query parameters - same path different params should return true', () => {
        expect(shouldFetchTargetData('/members/123?view=edit', '/members/123')).toBe(true);
    });

    it('handles query parameters - exact match including params should return false', () => {
        expect(shouldFetchTargetData('/members/123?view=edit', '/members/123?view=edit')).toBe(false);
    });
});
