import {describe, it, expect, vi, beforeEach} from 'vitest';
import type {MiddlewareCallbackParams} from 'openapi-fetch';

vi.mock('./klabisUserManager', async (importOriginal) => {
    const original = await importOriginal<typeof import('./klabisUserManager')>();
    const mockUserManager = {
        getUser: vi.fn().mockResolvedValue({access_token: 'initial-token'}),
        removeUser: vi.fn().mockResolvedValue(undefined),
        events: {
            addUserLoaded: vi.fn(),
            addUserUnloaded: vi.fn(),
            addAccessTokenExpired: vi.fn(),
            addSilentRenewError: vi.fn(),
            addUserSignedOut: vi.fn(),
        },
    };
    return {
        ...original,
        klabisAuthUserManager: mockUserManager,
        silentRenewRetry: vi.fn(),
        createUserManager: vi.fn(() => mockUserManager),
        authConfig: {
            authority: '/',
            client_id: 'klabis-web',
            redirect_uri: '/auth/callback',
            post_logout_redirect_uri: '/oauth/logout',
        }
    };
});

const {klabisAuthUserManager, silentRenewRetry} = await import('./klabisUserManager');
const mockGetUser = klabisAuthUserManager.getUser as ReturnType<typeof vi.fn>;
const mockSilentRenewRetry = silentRenewRetry as ReturnType<typeof vi.fn>;

// Import the exported middleware for direct testing
const {authMiddleware} = await import('./setup');

function makeMiddlewareParams(request: Request, response: Response): MiddlewareCallbackParams & { response: Response } {
    return {
        request,
        response,
        schemaPath: '/test',
        params: {},
        id: 'test-id',
        options: {baseUrl: '/api', fetch: globalThis.fetch, parseAs: 'json', querySerializer: vi.fn(), bodySerializer: vi.fn()},
    };
}

describe('setup.ts authMiddleware', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockGetUser.mockResolvedValue({access_token: 'initial-token'});
    });

    describe('onRequest', () => {
        it('adds Authorization header with current access token', async () => {
            const request = new Request('http://localhost/api/test');
            const result = await authMiddleware.onRequest!({
                request,
                schemaPath: '/test',
                params: {},
                id: 'test-id',
                options: {baseUrl: '/api', fetch: globalThis.fetch, parseAs: 'json', querySerializer: vi.fn(), bodySerializer: vi.fn()},
            });

            expect(result).toBeInstanceOf(Request);
            expect((result as Request).headers.get('Authorization')).toBe('Bearer initial-token');
        });

        it('throws when no access token is available', async () => {
            mockGetUser.mockResolvedValue(null);

            const request = new Request('http://localhost/api/test');
            await expect(
                authMiddleware.onRequest!({
                    request,
                    schemaPath: '/test',
                    params: {},
                    id: 'test-id',
                    options: {baseUrl: '/api', fetch: globalThis.fetch, parseAs: 'json', querySerializer: vi.fn(), bodySerializer: vi.fn()},
                })
            ).rejects.toThrow('No Klabis access_token available!');
        });
    });

    describe('onResponse', () => {
        it('passes through non-401 responses unchanged', async () => {
            const request = new Request('http://localhost/api/test');
            const response = new Response('ok', {status: 200});
            const params = makeMiddlewareParams(request, response);

            const result = await authMiddleware.onResponse!(params);

            expect(result).toBeUndefined();
            expect(mockSilentRenewRetry).not.toHaveBeenCalled();
        });

        it('on 401: calls silentRenewRetry and retries the request with new token', async () => {
            mockSilentRenewRetry.mockResolvedValue(undefined);
            // onResponse is invoked after the initial request already returned 401.
            // After silentRenewRetry succeeds, getUser() returns the freshly renewed token.
            mockGetUser.mockResolvedValue({access_token: 'new-token'});

            const originalRequest = new Request('http://localhost/api/test');
            const unauthorizedResponse = new Response('Unauthorized', {status: 401});
            const successResponse = new Response(JSON.stringify({data: 'ok'}), {status: 200});
            const mockFetch = vi.fn().mockResolvedValue(successResponse);
            vi.stubGlobal('fetch', mockFetch);

            const params = makeMiddlewareParams(originalRequest, unauthorizedResponse);
            const result = await authMiddleware.onResponse!(params);

            expect(mockSilentRenewRetry).toHaveBeenCalledTimes(1);
            expect(mockSilentRenewRetry).toHaveBeenCalledWith(klabisAuthUserManager);
            expect(mockFetch).toHaveBeenCalledTimes(1);

            const retryRequest = mockFetch.mock.calls[0][0] as Request;
            expect(retryRequest.headers.get('Authorization')).toBe('Bearer new-token');

            expect(result).toBe(successResponse);
        });

        it('on 401 when silentRenewRetry fails: throws the error', async () => {
            const renewError = new Error('all renew attempts exhausted');
            mockSilentRenewRetry.mockRejectedValue(renewError);

            const request = new Request('http://localhost/api/test');
            const response = new Response('Unauthorized', {status: 401});
            const params = makeMiddlewareParams(request, response);

            await expect(authMiddleware.onResponse!(params)).rejects.toThrow('all renew attempts exhausted');
        });

        it('on 401 followed by another 401 on retry: returns the 401 without infinite loop', async () => {
            mockSilentRenewRetry.mockResolvedValue(undefined);
            mockGetUser.mockResolvedValue({access_token: 'new-token'});

            const request = new Request('http://localhost/api/test');
            const unauthorizedResponse = new Response('Unauthorized', {status: 401});
            const secondUnauthorizedResponse = new Response('Unauthorized again', {status: 401});
            const mockFetch = vi.fn().mockResolvedValue(secondUnauthorizedResponse);
            vi.stubGlobal('fetch', mockFetch);

            const params = makeMiddlewareParams(request, unauthorizedResponse);
            const result = await authMiddleware.onResponse!(params);

            // Returns the 401 response from the retry — no second renew attempt
            expect(result).toBe(secondUnauthorizedResponse);
            expect(mockSilentRenewRetry).toHaveBeenCalledTimes(1);
            expect(mockFetch).toHaveBeenCalledTimes(1);
        });
    });
});
