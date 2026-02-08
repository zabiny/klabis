import {type FormValidationError, isFormValidationError, toFormValidationError} from './hateoas';
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

        it('should return original FetchError for non-400 status', () => {
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
