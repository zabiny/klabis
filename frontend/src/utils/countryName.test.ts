import {countryLabel, countryName} from './countryName';

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

describe('countryLabel', () => {
    it('uses the Czech name for a known code', () => {
        expect(countryLabel('FR')).toBe('Francie');
    });

    it.each(['QQ', null, undefined])('falls back to the raw value %s', (code) => {
        expect(countryLabel(code)).toBe(code);
    });
});
