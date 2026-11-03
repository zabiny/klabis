const MAX_LENGTH = 2048;
const FALLBACK = '/';
const NON_RETURNABLE_PATHS = ['/auth/callback', '/login'];

// The value comes from browser storage, so it is re-validated before use. Only same-origin, app-relative paths are allowed.
export const toSafeReturnPath = (value: unknown): string => {
    if (typeof value !== 'string' || value.length === 0 || value.length > MAX_LENGTH) {
        return FALLBACK;
    }
    if (!value.startsWith('/') || value.startsWith('//') || value.startsWith('/\\')) {
        return FALLBACK;
    }
    // eslint-disable-next-line no-control-regex
    if (/[\u0000-\u001f\u007f]/.test(value)) {
        return FALLBACK;
    }

    let url: URL;
    try {
        url = new URL(value, window.location.origin);
    } catch {
        return FALLBACK;
    }
    if (url.origin !== window.location.origin) {
        return FALLBACK;
    }
    if (NON_RETURNABLE_PATHS.includes(url.pathname)) {
        return FALLBACK;
    }
    return url.pathname + url.search + url.hash;
};
