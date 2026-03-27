import '@testing-library/jest-dom';
import {beforeEach, vi} from 'vitest';

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
