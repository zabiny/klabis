import {
    type FormValidationError,
    isFormValidationError,
    parseHalHref,
    serializeHalHref,
    toFormValidationError,
} from './hateoas';
import {FetchError} from './authorizedFetch';

describe('toFormValidationError', () => {
    describe('FetchError with 400 + problem+json', () => {
        it('should convert FetchError to FormValidationError', () => {
            const headers = new Headers({'Content-Type': 'application/problem+json'});
            const responseBody = JSON.stringify({
                errors: {
                    name: 'Name is required',
                    email: 'Invalid email format',
                },
            });

            const fetchError = new FetchError(
                'HTTP 400: Bad Request',
                400,
                'Bad Request',
                headers,
                responseBody
            );

            const error = toFormValidationError(fetchError);

            expect(error.message).toBe('Form validation errors');
            expect(isFormValidationError(error)).toBeTruthy();

            const validationError = error as FormValidationError;
            expect(validationError.validationErrors).toEqual({
                name: 'Name is required',
                email: 'Invalid email format',
            });
        });

        it('should handle empty errors object', () => {
            const headers = new Headers({'Content-Type': 'application/problem+json'});
            const responseBody = JSON.stringify({errors: {}});

            const fetchError = new FetchError(
                'HTTP 400: Bad Request',
                400,
                'Bad Request',
                headers,
                responseBody
            );

            const error = toFormValidationError(fetchError);

            expect(isFormValidationError(error)).toBeTruthy();
            const validationError = error as FormValidationError;
            expect(validationError.validationErrors).toEqual({});
        });

        it('should handle missing errors field in response', () => {
            const headers = new Headers({'Content-Type': 'application/problem+json'});
            const responseBody = JSON.stringify({});

            const fetchError = new FetchError(
                'HTTP 400: Bad Request',
                400,
                'Bad Request',
                headers,
                responseBody
            );

            const error = toFormValidationError(fetchError);

            expect(isFormValidationError(error)).toBeTruthy();
            const validationError = error as FormValidationError;
            expect(validationError.validationErrors).toEqual({});
        });

        it('should handle invalid JSON in response body', () => {
            const headers = new Headers({'Content-Type': 'application/problem+json'});
            const fetchError = new FetchError(
                'HTTP 400: Bad Request',
                400,
                'Bad Request',
                headers,
                'Invalid JSON'
            );

            const error = toFormValidationError(fetchError);

            // Should return the original FetchError when JSON parsing fails
            expect(error).toEqual(fetchError);
        });
    });

    describe('FetchError without validation errors', () => {
        it('should return original FetchError for 400 + other content-type', () => {
            const headers = new Headers({'Content-Type': 'application/json'});
            const fetchError = new FetchError(
                'HTTP 400: Bad Request',
                400,
                'Bad Request',
                headers,
                'Some error'
            );

            const error = toFormValidationError(fetchError);

            expect(error).toEqual(fetchError);
            expect(isFormValidationError(error)).toBeFalsy();
        });

        it('should return original FetchError for non-400/non-409 status', () => {
            const headers = new Headers({'Content-Type': 'application/problem+json'});
            const fetchError = new FetchError(
                'HTTP 500: Server Error',
                500,
                'Internal Server Error',
                headers,
                'Server error'
            );

            const error = toFormValidationError(fetchError);

            expect(error).toEqual(fetchError);
            expect(isFormValidationError(error)).toBeFalsy();
        });

        it('should extract detail message from 409 + problem+json conflict response', () => {
            const headers = new Headers({'Content-Type': 'application/problem+json'});
            const responseBody = JSON.stringify({
                title: 'Member Already In Training Group',
                detail: 'Member abc123 is already a trainee of training group xyz456',
                status: 409,
            });

            const fetchError = new FetchError(
                'HTTP 409 ()',
                409,
                '',
                headers,
                responseBody
            );

            const error = toFormValidationError(fetchError);

            expect(error.message).toBe('Member abc123 is already a trainee of training group xyz456');
            expect(isFormValidationError(error)).toBeFalsy();
        });

        it('should return original FetchError for missing content-type', () => {
            const headers = new Headers();
            const fetchError = new FetchError(
                'HTTP 400: Bad Request',
                400,
                'Bad Request',
                headers,
                'Some error'
            );

            const error = toFormValidationError(fetchError);

            expect(error).toEqual(fetchError);
            expect(isFormValidationError(error)).toBeFalsy();
        });
    });

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

    describe('Non-FetchError objects', () => {
        it('should return Error unchanged if already Error', () => {
            const error = new Error('Some error');

            const result = toFormValidationError(error);

            expect(result).toEqual(error);
            expect(isFormValidationError(result)).toBeFalsy();
        });

        it('should wrap non-Error objects in Error', () => {
            const result = toFormValidationError('String error');

            expect(result).toBeInstanceOf(Error);
            expect(result.message).toBe('String error');
            expect(isFormValidationError(result)).toBeFalsy();
        });

        it('should wrap null in Error', () => {
            const result = toFormValidationError(null);

            expect(result).toBeInstanceOf(Error);
            expect(result.message).toBe('null');
            expect(isFormValidationError(result)).toBeFalsy();
        });

        it('should wrap undefined in Error', () => {
            const result = toFormValidationError(undefined);

            expect(result).toBeInstanceOf(Error);
            expect(result.message).toBe('undefined');
            expect(isFormValidationError(result)).toBeFalsy();
        });
    });
});
