/**
 * Shared utilities for mocking fetch in tests
 * Provides consistent Response mock objects across all test files
 */

/**
 * Creates a mock Response object that mimics real fetch API behavior
 * @param data - The JSON data to be returned by response.json()
 * @param status - HTTP status code (default: 200)
 * @returns Mock Response object
 */
export const createMockResponse = (data: any, status = 200): Response => {
    const createResponse = (): Response => ({
        ok: status >= 200 && status < 300,
        status,
        statusText: getStatusText(status),
        json: async () => data,
        text: async () => JSON.stringify(data),
        blob: async () => new Blob([JSON.stringify(data)]),
        arrayBuffer: async () => new TextEncoder().encode(JSON.stringify(data)).buffer,
        formData: async () => new FormData(),
        clone: createResponse,
        headers: new Headers(),
        redirected: false,
        type: 'basic' as ResponseType,
        url: '',
        body: null,
        bodyUsed: false,
    } as Response);

    return createResponse();
};

/**
 * Gets standard HTTP status text for a status code
 */
function getStatusText(status: number): string {
    const statusTexts: Record<number, string> = {
        200: 'OK',
        201: 'Created',
        204: 'No Content',
        400: 'Bad Request',
        401: 'Unauthorized',
        403: 'Forbidden',
        404: 'Not Found',
        405: 'Method Not Allowed',
        500: 'Internal Server Error',
        502: 'Bad Gateway',
        503: 'Service Unavailable',
    };
    return statusTexts[status] || 'Unknown';
}

/**
 * Helper to create a delayed mock response for testing loading states
 * @param data - The JSON data to be returned
 * @param delayMs - Delay in milliseconds
 * @returns Promise that resolves with a mock Response
 */
export const createDelayedMockResponse = (data: any, delayMs = 100): Promise<Response> => {
    return new Promise((resolve) =>
        setTimeout(() => resolve(createMockResponse(data)), delayMs)
    );
};
