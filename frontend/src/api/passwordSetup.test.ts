import {afterEach, describe, expect, it, vi} from 'vitest';
import {ApiError} from './apiError';
import {completePasswordSetup, RateLimitError, requestNewToken, validateToken} from './passwordSetup';

const problemResponse = (status: number, body: Record<string, unknown>, headers: Record<string, string> = {}) =>
    new Response(JSON.stringify(body), {
        status,
        headers: {'Content-Type': 'application/problem+json', ...headers},
    });

describe('passwordSetup api', () => {
    afterEach(() => {
        vi.unstubAllGlobals();
    });

    it('throws ApiError with type and params when token validation fails', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
            problemResponse(410, {type: '/errors/token-expired', detail: 'Token expired'})));

        const error = await validateToken('abc').catch((e: unknown) => e);

        expect(error).toBeInstanceOf(ApiError);
        expect((error as ApiError).status).toBe(410);
        expect((error as ApiError).type).toBe('/errors/token-expired');
    });

    it('throws ApiError with violations when the new password is rejected', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
            problemResponse(400, {type: '/errors/password-requirements-not-met', violations: ['TOO_SHORT'], minLength: 12})));

        const error = await completePasswordSetup({token: 't', password: 'x', passwordConfirmation: 'x'})
            .catch((e: unknown) => e);

        expect((error as ApiError).type).toBe('/errors/password-requirements-not-met');
        expect((error as ApiError).params).toEqual({violations: ['TOO_SHORT'], minLength: 12});
    });

    it('throws ApiError carrying the field of a field-bound error', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
            problemResponse(400, {type: '/errors/passwords-do-not-match', field: 'passwordConfirmation'})));

        const error = await completePasswordSetup({token: 't', password: 'x', passwordConfirmation: 'y'})
            .catch((e: unknown) => e);

        expect((error as ApiError).field).toBe('passwordConfirmation');
    });

    it('throws ApiError with only status when body is not problem+json', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('<html/>', {status: 502})));

        const error = await requestNewToken({registrationNumber: 'ZBM1', email: 'a@b.cz'}).catch((e: unknown) => e);

        expect(error).toBeInstanceOf(ApiError);
        expect((error as ApiError).status).toBe(502);
        expect((error as ApiError).type).toBeUndefined();
    });

    it('keeps RateLimitError with the Retry-After value', async () => {
        vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
            problemResponse(429, {type: '/errors/rate-limit-exceeded'}, {'Retry-After': '30'})));

        const error = await requestNewToken({registrationNumber: 'ZBM1', email: 'a@b.cz'}).catch((e: unknown) => e);

        expect(error).toBeInstanceOf(RateLimitError);
        expect((error as RateLimitError).retryAfter).toBe(30);
    });
});
