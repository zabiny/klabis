import {describe, expect, it} from 'vitest';
import {ApiError} from '../api/authorizedFetch';
import {describeError} from './describeError';
import {labels} from './labels';

describe('describeError', () => {
    it('uses the Czech template of a known type', () => {
        const error = new ApiError(400, {type: '/errors/voting-deadline-not-in-future', params: {deadline: '2026-10-18'}});

        expect(describeError(error)).toBe('Termín pro výběr musí být v budoucnosti.');
    });

    it('passes params to the template', () => {
        const original = labels.errorTypes['/errors/conflict'];
        let received: unknown;
        (labels.errorTypes as Record<string, unknown>)['/errors/conflict'] = (p: unknown) => {
            received = p;
            return 'x';
        };
        try {
            describeError(new ApiError(409, {type: '/errors/conflict', params: {a: 1}}));
        } finally {
            (labels.errorTypes as Record<string, unknown>)['/errors/conflict'] = original;
        }

        expect(received).toEqual({a: 1});
    });

    it.each([
        [400, '/errors/invalid-request'],
        [401, '/errors/unauthorized'],
        [403, '/errors/forbidden'],
        [404, '/errors/not-found'],
        [409, '/errors/conflict'],
        [500, '/errors/unexpected'],
        [503, '/errors/unexpected'],
    ] as const)('falls back to the generic text for status %i when the type is unknown', (status, type) => {
        const error = new ApiError(status, {type: '/errors/something-new', detail: 'English detail'});

        expect(describeError(error)).toBe(labels.errorTypes[type]({}));
    });

    it('falls back by status when there is no type', () => {
        expect(describeError(new ApiError(404))).toBe(labels.errorTypes['/errors/not-found']({}));
    });

    it('never exposes detail or message', () => {
        const text = describeError(new ApiError(418, {detail: 'I am a teapot'}));

        expect(text).toBe(labels.errors.unexpectedError);
    });

    it.each([
        ['a plain Error', new Error('Failed to fetch')],
        ['a string', 'boom'],
        ['null', null],
    ])('gives unexpectedError for %s', (_name, error) => {
        expect(describeError(error)).toBe(labels.errors.unexpectedError);
    });

    it('has a non-empty Czech text for every known type', () => {
        for (const template of Object.values(labels.errorTypes)) {
            expect(template({})).not.toBe('');
        }
    });
});
