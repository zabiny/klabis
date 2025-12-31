// Rozšíření matcherů pro DOM od @testing-library/jest-dom
import '@testing-library/jest-dom';
import {afterAll, afterEach, beforeAll, beforeEach, vi} from 'vitest';

// Set up globalThis.__DEV__ for getApiBaseUrl()
// Vite will replace this at build time in production/dev builds,
// but in Vitest tests we default to false (production-like behavior)
(globalThis as any).__DEV__ = false;

// Mock klabisUserManager to avoid initialization errors in tests
vi.mock('./api/klabisUserManager', () => {
    const mockUserManager = {
        getUser: vi.fn().mockResolvedValue(null),
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
            client_id: 'frontend',
            client_secret: 'fesecret',
            redirect_uri: '/auth/callback',
            post_logout_redirect_uri: '/oauth/logout',
            response_type: 'code',
            scope: 'openid profile email'
        }
    };
});


// Mock pro případné globální objekty nebo API
// global.ResizeObserver = vi.fn().mockImplementation(() => ({
//     observe: vi.fn(),
//     unobserve: vi.fn(),
//     disconnect: vi.fn(),
// }));

// Vitest global setup
beforeAll(() => {
    // Globální setup před všemi testy
});

afterAll(() => {
    // Globální cleanup po všech testech
});

beforeEach(() => {
    // Setup před každým testem
    vi.clearAllMocks();
});

afterEach(() => {
    // Cleanup po každém testu
});
