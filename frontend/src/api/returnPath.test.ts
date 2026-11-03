import {describe, expect, it} from 'vitest';
import {toSafeReturnPath} from './returnPath';

describe('toSafeReturnPath', () => {
    it.each([
        ['/events/42', '/events/42'],
        ['/members?q=x#top', '/members?q=x#top'],
        ['/', '/'],
    ])('accepts %s', (input, expected) => {
        expect(toSafeReturnPath(input)).toBe(expected);
    });

    it.each([
        ['undefined', undefined],
        ['null', null],
        ['number', 42],
        ['object', {}],
        ['empty string', ''],
        ['relative path', 'events'],
        ['protocol-relative', '//evil.example'],
        ['backslash host', '/\\evil.example'],
        ['absolute url', 'https://evil.example/x'],
        ['javascript scheme', 'javascript:alert(1)'],
        ['newline', '/events/\n42'],
        ['tab', '/events/\t42'],
        ['null char', '/events/\u000042'],
        ['over-long', '/' + 'a'.repeat(3000)],
        ['callback route', '/auth/callback?code=x'],
        ['callback route bare', '/auth/callback'],
        ['login', '/login'],
        ['login with query', '/login?error'],
    ])('returns / for %s', (_name, input) => {
        expect(toSafeReturnPath(input)).toBe('/');
    });
});
