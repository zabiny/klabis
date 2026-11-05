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
const {authorizedFetch, ApiError} = await import('./authorizedFetch');

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

        await expect(authorizedFetch('/test')).rejects.toBeInstanceOf(ApiError);

        // silentRenewRetry called only once (for the first 401), not for the retried 401
        expect(mockSilentRenewRetry).toHaveBeenCalledTimes(1);
        expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('throws ApiError for non-401 error responses when throwOnError is true', async () => {
        const mockFetch = vi.fn().mockResolvedValue(new Response('Not Found', {status: 404}));
        vi.stubGlobal('fetch', mockFetch);

        await expect(authorizedFetch('/test')).rejects.toBeInstanceOf(ApiError);
        expect(mockSilentRenewRetry).not.toHaveBeenCalled();
    });

    it('prepends /api prefix to relative URLs not starting with /api', async () => {
        const mockFetch = vi.fn().mockResolvedValue(new Response('ok', {status: 200}));
        vi.stubGlobal('fetch', mockFetch);

        await authorizedFetch('/members');

        expect(mockFetch.mock.calls[0][0]).toBe('/api/members');
    });

    it.each([
        ['raw absolute HAL href', 'https://localhost:8443/api/members/1', '/api/members/1'],
        ['templated HAL href', '/api/members{?q,page}', '/api/members'],
        ['templated href keeping literal query', '/api/members?sort=name{&page}', '/api/members?sort=name'],
        ['bare route path', 'members/1', '/api/members/1'],
    ])('resolves %s to an origin-relative /api URL', async (_label, input, expected) => {
        const mockFetch = vi.fn().mockResolvedValue(new Response('ok', {status: 200}));
        vi.stubGlobal('fetch', mockFetch);

        await authorizedFetch(input);

        expect(mockFetch.mock.calls[0][0]).toBe(expected);
    });

    it('passes a URL instance through unchanged', async () => {
        const mockFetch = vi.fn().mockResolvedValue(new Response('ok', {status: 200}));
        vi.stubGlobal('fetch', mockFetch);

        await authorizedFetch(new URL('https://example.com/other?x=1'));

        expect(mockFetch.mock.calls[0][0]).toBe('https://example.com/other?x=1');
    });

    describe('error parsing', () => {
        const problemResponse = (body: unknown, status = 400, contentType = 'application/problem+json') =>
            new Response(typeof body === 'string' ? body : JSON.stringify(body), {
                status,
                headers: {'Content-Type': contentType},
            });

        const failWith = async (response: Response): Promise<InstanceType<typeof ApiError>> => {
            vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response));
            return await authorizedFetch('/test').catch(e => e);
        };

        it('maps every documented problem+json member', async () => {
            const error = await failWith(problemResponse({
                type: '/errors/voting-deadline-not-in-future',
                title: 'Bad Request',
                status: 400,
                detail: 'Voting deadline must be in the future',
                instance: '/api/x',
                field: 'votingDeadline',
                fieldErrors: {name: 'nesmí být prázdné'},
                errors: ['global one'],
                parameterErrors: ['param one'],
                deadline: '2026-10-18',
            }));

            expect(error).toBeInstanceOf(ApiError);
            expect(error.status).toBe(400);
            expect(error.type).toBe('/errors/voting-deadline-not-in-future');
            expect(error.detail).toBe('Voting deadline must be in the future');
            expect(error.field).toBe('votingDeadline');
            expect(error.fieldErrors).toEqual({name: 'nesmí být prázdné'});
            expect(error.errors).toEqual(['global one', 'param one']);
            expect(error.params).toEqual({deadline: '2026-10-18'});
            expect(error.message).toBe('HTTP 400: Voting deadline must be in the future');
        });

        it('puts unknown extension properties into params', async () => {
            const error = await failWith(problemResponse({
                type: '/errors/conflict',
                status: 409,
                groups: {affectedGroups: []},
                somethingNew: 42,
            }, 409));

            expect(error.params).toEqual({groups: {affectedGroups: []}, somethingNew: 42});
        });

        it('defaults collections to empty and omits detail from message', async () => {
            const error = await failWith(problemResponse({type: '/errors/not-found', status: 404}, 404));

            expect(error.fieldErrors).toEqual({});
            expect(error.errors).toEqual([]);
            expect(error.params).toEqual({});
            expect(error.message).toBe('HTTP 404');
        });

        it('does not throw when the problem+json body is not valid JSON', async () => {
            const error = await failWith(problemResponse('<html>oops</html>', 500));

            expect(error).toBeInstanceOf(ApiError);
            expect(error.status).toBe(500);
            expect(error.type).toBeUndefined();
            expect(error.fieldErrors).toEqual({});
            expect(error.errors).toEqual([]);
            expect(error.params).toEqual({});
        });

        it('ignores a JSON body whose content type is not problem+json', async () => {
            const error = await failWith(problemResponse({type: '/errors/conflict', detail: 'x'}, 409, 'application/json'));

            expect(error.status).toBe(409);
            expect(error.type).toBeUndefined();
            expect(error.detail).toBeUndefined();
            expect(error.message).toBe('HTTP 409');
        });

        it('exposes no raw response data', async () => {
            const error = await failWith(problemResponse({status: 400}));

            expect(error).not.toHaveProperty('responseBody');
            expect(error).not.toHaveProperty('responseHeaders');
            expect(error).not.toHaveProperty('responseStatusText');
        });
    });
});
