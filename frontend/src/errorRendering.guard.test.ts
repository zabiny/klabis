import {readdirSync, readFileSync, statSync} from 'node:fs';
import {join, relative} from 'node:path';
import {describe, expect, it} from 'vitest';

const SRC = join(__dirname);
const EXCLUDED = new Set(['api/authorizedFetch.ts', 'api/problemFetch.ts', 'api/apiError.ts']);

const FORBIDDEN: Array<[string, RegExp]> = [
    ['rendering an error message in JSX', /\{\s*\(?\w*[eE]rr\w*\)?\??\.message\s*\}/],
    ['casting an error to read its message', /\(\w*[eE]rr\w*\s+as\s+Error\)\.message/],
    ['reading a raw response body of an error', /\bresponseBody\b/],
    ['passing an error message to a toast or setter', /(addToast|setSubmitError|setServerError)\(\s*\w*[eE]rr\w*\??\.message/],
];

const sourceFiles = (dir: string): string[] =>
    readdirSync(dir).flatMap((name) => {
        const path = join(dir, name);
        if (statSync(path).isDirectory()) return sourceFiles(path);
        return /\.(ts|tsx)$/.test(name) && !/\.(test|spec)\.(ts|tsx)$/.test(name) && !name.endsWith('.d.ts') ? [path] : [];
    });

describe('error rendering guard', () => {
    it.each(FORBIDDEN)('source does not contain: %s', (_description, pattern) => {
        const offenders = sourceFiles(SRC)
            .filter((file) => !EXCLUDED.has(relative(SRC, file)))
            .filter((file) => pattern.test(readFileSync(file, 'utf-8')))
            .map((file) => relative(SRC, file));

        expect(offenders).toEqual([]);
    });
});
