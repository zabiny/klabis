import {parseHalHref, serializeHalHref} from './hateoas';

describe('hateoas', () => {
    describe('parseHalHref', () => {
        it('resolves a relative HAL href against the current origin', () => {
            const url = parseHalHref('/api/events?year=2026&when=budouci');

            expect(url.origin).toBe(window.location.origin);
            expect(url.pathname).toBe('/api/events');
            expect(url.searchParams.get('year')).toBe('2026');
            expect(url.searchParams.get('when')).toBe('budouci');
        });

        it('leaves an absolute HAL href unchanged', () => {
            const url = parseHalHref('https://api.example.com/api/events?year=2026');

            expect(url.origin).toBe('https://api.example.com');
            expect(url.pathname).toBe('/api/events');
            expect(url.searchParams.get('year')).toBe('2026');
        });

        it('does not throw on a relative href (regression: "Failed to construct URL")', () => {
            expect(() => parseHalHref('/api/members')).not.toThrow();
        });
    });

    describe('serializeHalHref', () => {
        it('returns an origin-relative href with the mutated query string', () => {
            const url = parseHalHref('/api/events?year=2026');
            url.searchParams.set('page', '0');

            expect(serializeHalHref(url)).toBe('/api/events?year=2026&page=0');
        });

        it('drops the origin even when the href was parsed from an absolute URL', () => {
            const url = parseHalHref('https://api.example.com/api/events?year=2026');
            url.searchParams.set('page', '0');

            expect(serializeHalHref(url)).toBe('/api/events?year=2026&page=0');
        });

        it('round-trips a relative href with no query string', () => {
            expect(serializeHalHref(parseHalHref('/api/members'))).toBe('/api/members');
        });

        it('preserves a hash fragment', () => {
            expect(serializeHalHref(parseHalHref('/api/members?page=0#section'))).toBe('/api/members?page=0#section');
        });
    });
});
