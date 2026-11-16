import {countryName} from './countryName';

describe('countryName', () => {
    it.each([
        ['CZ', 'Česko'],
        ['FR', 'Francie'],
        ['cz', 'Česko'],
    ])('resolves %s to %s', (code, name) => {
        expect(countryName(code)).toBe(name);
    });

    it.each(['QQ', '', 'C', 'CZE', '12', null, undefined])('returns nothing for %s', (code) => {
        expect(countryName(code)).toBeUndefined();
    });
});
