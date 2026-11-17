import {asLinkArray, parseHalHref, selfHref, serializeHalHref, toHref} from './hateoas';

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

describe('selfHref', () => {
    it('returns the href of the self link', () => {
        expect(selfHref({_links: {self: {href: '/api/things/1'}}})).toBe('/api/things/1');
    });

    it('returns undefined without a self link or resource', () => {
        expect(selfHref({_links: {}})).toBeUndefined();
        expect(selfHref(null)).toBeUndefined();
        expect(selfHref(undefined)).toBeUndefined();
    });
});

describe('toHref', () => {
    it('returns the href of a single link', () => {
        expect(toHref({href: '/api/things/1'})).toBe('/api/things/1');
    });

    it('passes a string through unchanged', () => {
        expect(toHref('/api/things/1' as never)).toBe('/api/things/1');
    });

    it('takes the first element of an array relation', () => {
        expect(toHref([{href: '/api/things/1'}, {href: '/api/things/2'}])).toBe('/api/things/1');
    });

    it('recurses into a nested array', () => {
        expect(toHref([[{href: '/api/things/1'}]] as never)).toBe('/api/things/1');
    });

    it('throws for an empty array', () => {
        expect(() => toHref([])).toThrow(/Unknown NavigationTarget/);
    });

    it('throws for an unknown shape', () => {
        expect(() => toHref({} as never)).toThrow(/Unknown NavigationTarget/);
        expect(() => toHref(null as never)).toThrow(/Unknown NavigationTarget/);
        expect(() => toHref(42 as never)).toThrow(/Unknown NavigationTarget/);
    });

    it('throws for a link without an href', () => {
        expect(() => toHref({href: ''})).toThrow(/Chybi hodnota href/);
    });
});

describe('asLinkArray', () => {
    it('returns an empty array for missing links', () => {
        expect(asLinkArray(undefined)).toEqual([]);
        expect(asLinkArray(null)).toEqual([]);
    });

    it('wraps a single link', () => {
        const link = {href: '/api/things/1'};
        expect(asLinkArray(link)).toEqual([link]);
    });

    it('returns an array relation as is', () => {
        const links = [{href: '/api/things/1'}, {href: '/api/things/2'}];
        expect(asLinkArray(links)).toEqual(links);
    });

    it('returns an empty array unchanged', () => {
        expect(asLinkArray([])).toEqual([]);
    });
});
