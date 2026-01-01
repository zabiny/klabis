/**
 * Returns the appropriate API base URL based on the environment.
 *
 * - Development: '/api' (proxied by Vite to https://localhost:8443)
 * - Production: '' (relative paths, same domain as frontend)
 *
 * Vite automatically injects `globalThis.__DEV__` at build time.
 * Jest tests default to false (production-like behavior).
 */
export function getApiBaseUrl(): string {
    // Access globalThis.__DEV__ instead of import.meta to avoid ts-jest parsing issues
    // Vite will replace this reference with a compile-time constant in builds
    const g = globalThis as any;
    return g.__DEV__ ? '/api' : '';
}
