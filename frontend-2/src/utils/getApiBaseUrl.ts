/**
 * Returns the appropriate API base URL based on the environment.
 *
 * - Development: '/api' (proxied by Vite to https://localhost:8443)
 * - Production: '' (relative paths, same domain as frontend)
 */
export function getApiBaseUrl(): string {
    return import.meta.env.DEV ? '/api' : '';
}
