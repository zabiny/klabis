import {describe, it, expect, vi, beforeEach} from 'vitest';

vi.mock('./klabisUserManager', () => {
    const mockUserManager = {
        getUser: vi.fn(),
        removeUser: vi.fn().mockResolvedValue(undefined),
        signinSilent: vi.fn(),
    };
    return {
        klabisAuthUserManager: mockUserManager,
        silentRenewRetry: vi.fn(),
    };
});

const {klabisAuthUserManager, silentRenewRetry} = await import('./klabisUserManager');
const {authorizedFetch, FetchError} = await import('./authorizedFetch');

const mockUserManager = klabisAuthUserManager as unknown as {
    getUser: ReturnType<typeof vi.fn>;
    removeUser: ReturnType<typeof vi.fn>;
};
const mockSilentRenewRetry = silentRenewRetry as ReturnType<typeof vi.fn>;

describe('authorizedFetch', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockUserManager.getUser.mockResolvedValue({access_token: 'initial-token'});
    });

    it('adds Bearer token from klabisAuthUserManager to request headers', async () => {
        const mockFetch = vi.fn().mockResolvedValue(new Response('ok', {status: 200}));
        vi.stubGlobal('fetch', mockFetch);

        await authorizedFetch('/test');

        const [, options] = mockFetch.mock.calls[0];
        const headers = options.headers as Record<string, string>;
        expect(headers['Authorization']).toBe('Bearer initial-token');
    });

    it('on 401: calls silentRenewRetry and retries the original request with new token', async () => {
        mockUserManager.getUser
            .mockResolvedValueOnce({access_token: 'expired-token'})
            .mockResolvedValueOnce({access_token: 'new-token'});
        mockSilentRenewRetry.mockResolvedValue(undefined);

        const mockFetch = vi.fn()
            .mockResolvedValueOnce(new Response('Unauthorized', {status: 401}))
            .mockResolvedValueOnce(new Response('ok', {status: 200}));
        vi.stubGlobal('fetch', mockFetch);

        const response = await authorizedFetch('/test');

        expect(mockSilentRenewRetry).toHaveBeenCalledTimes(1);
        expect(mockSilentRenewRetry).toHaveBeenCalledWith(klabisAuthUserManager);
        expect(mockFetch).toHaveBeenCalledTimes(2);

        const [, secondCallOptions] = mockFetch.mock.calls[1];
        const retryHeaders = secondCallOptions.headers as Record<string, string>;
        expect(retryHeaders['Authorization']).toBe('Bearer new-token');

        expect(response.status).toBe(200);
    });

    it('on 401: does NOT call removeUser directly — delegates to silentRenewRetry', async () => {
        mockSilentRenewRetry.mockResolvedValue(undefined);
        mockUserManager.getUser.mockResolvedValue({access_token: 'some-token'});

        const mockFetch = vi.fn()
            .mockResolvedValueOnce(new Response('Unauthorized', {status: 401}))
            .mockResolvedValueOnce(new Response('ok', {status: 200}));
        vi.stubGlobal('fetch', mockFetch);

        await authorizedFetch('/test');

        expect(mockUserManager.removeUser).not.toHaveBeenCalled();
    });

    it('on 401 when silentRenewRetry fails: throws the error without retrying the request', async () => {
        const renewError = new Error('all renew attempts exhausted');
        mockSilentRenewRetry.mockRejectedValue(renewError);

        const mockFetch = vi.fn()
            .mockResolvedValueOnce(new Response('Unauthorized', {status: 401}));
        vi.stubGlobal('fetch', mockFetch);

        await expect(authorizedFetch('/test')).rejects.toThrow('all renew attempts exhausted');

        // Only the original request, no retry after failed renewal
        expect(mockFetch).toHaveBeenCalledTimes(1);
    });

    it('on 401 followed by another 401 on retry: does not trigger a second silentRenewRetry', async () => {
        mockSilentRenewRetry.mockResolvedValue(undefined);
        mockUserManager.getUser.mockResolvedValue({access_token: 'new-token'});

        const mockFetch = vi.fn()
            .mockResolvedValue(new Response('Unauthorized', {status: 401}));
        vi.stubGlobal('fetch', mockFetch);

        await expect(authorizedFetch('/test')).rejects.toBeInstanceOf(FetchError);

        // silentRenewRetry called only once (for the first 401), not for the retried 401
        expect(mockSilentRenewRetry).toHaveBeenCalledTimes(1);
        expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('throws FetchError for non-401 error responses when throwOnError is true', async () => {
        const mockFetch = vi.fn().mockResolvedValue(new Response('Not Found', {status: 404}));
        vi.stubGlobal('fetch', mockFetch);

        await expect(authorizedFetch('/test')).rejects.toBeInstanceOf(FetchError);
        expect(mockSilentRenewRetry).not.toHaveBeenCalled();
    });

    it('prepends /api prefix to relative URLs not starting with /api', async () => {
        const mockFetch = vi.fn().mockResolvedValue(new Response('ok', {status: 200}));
        vi.stubGlobal('fetch', mockFetch);

        await authorizedFetch('/members');

        expect(mockFetch.mock.calls[0][0]).toBe('/api/members');
    });
});
