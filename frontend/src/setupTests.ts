import '@testing-library/jest-dom';
import {beforeEach, vi} from 'vitest';

// Global ResizeObserver mock
window.ResizeObserver = vi.fn().mockImplementation(() => ({
    observe: vi.fn(),
    unobserve: vi.fn(),
    disconnect: vi.fn(),
}))

// Global matchMedia mock (defaults to desktop)
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    })),
});

vi.mock('./api/klabisUserManager', () => {
    const mockUserManager = {
        getUser: vi.fn().mockReturnValue(null),
        events: {
            addUserLoaded: vi.fn(),
            addUserUnloaded: vi.fn(),
            addAccessTokenExpired: vi.fn(),
            addSilentRenewError: vi.fn(),
            addUserSignedOut: vi.fn(),
        },
        signinSilent: vi.fn().mockResolvedValue(null),
    };

    return {
        klabisAuthUserManager: mockUserManager,
        createUserManager: vi.fn(() => mockUserManager),
        authConfig: {
            authority: '/',
            client_id: 'klabis-web',
            redirect_uri: '/auth/callback',
            post_logout_redirect_uri: '/oauth/logout',
            response_type: 'code',
            scope: 'openid profile email'
        }
    };
});

beforeEach(() => {
    vi.clearAllMocks();
});
