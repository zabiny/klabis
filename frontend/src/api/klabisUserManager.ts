import {User, UserManager, WebStorageStateStore,} from 'oidc-client-ts';
import {normalizeUrl} from "./hateoas.ts";
import {hideRenewalOverlay, showRenewalOverlay} from "./tokenRenewalState.ts";

export interface AuthConfig {
    authority: string;
    client_id: string;
    redirect_uri: string;
    post_logout_redirect_uri: string;
    response_type?: string;
    scope?: string;
    onUserLoaded?: (user: User) => void;
    onUserUnloaded?: () => void;
}

const SILENT_RENEW_MAX_ATTEMPTS = 3;
const SILENT_RENEW_RETRY_DELAY_MS = 5000;

/**
 * Tracks the active retry promise so concurrent callers share the same cycle
 * rather than stacking independent retry loops.
 */
let activeRetryPromise: Promise<void> | null = null;

const delay = (ms: number): Promise<void> =>
    new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Attempts signinSilent up to SILENT_RENEW_MAX_ATTEMPTS times with a fixed delay
 * between attempts. Calls removeUser() only after all attempts are exhausted.
 *
 * Exported so authorizedFetch 401 handling can reuse the same strategy.
 */
export const silentRenewRetry = (userManager: UserManager): Promise<void> => {
    if (activeRetryPromise !== null) {
        return activeRetryPromise;
    }

    const execute = async (): Promise<void> => {
        const existingUser = await userManager.getUser();
        if (!existingUser || existingUser.expired) {
            throw new Error('No active session — silent renew is not applicable');
        }

        showRenewalOverlay();
        let lastError: unknown;
        try {
            for (let attempt = 1; attempt <= SILENT_RENEW_MAX_ATTEMPTS; attempt++) {
                try {
                    await userManager.signinSilent();
                    return;
                } catch (err) {
                    lastError = err;
                    console.warn(`Silent renew attempt ${attempt}/${SILENT_RENEW_MAX_ATTEMPTS} failed`, err);
                    if (attempt < SILENT_RENEW_MAX_ATTEMPTS) {
                        await delay(SILENT_RENEW_RETRY_DELAY_MS);
                    }
                }
            }
            console.error('All silent renew attempts exhausted, logging out', lastError);
            await userManager.removeUser();
            throw lastError;
        } finally {
            hideRenewalOverlay();
        }
    };

    activeRetryPromise = execute().finally(() => {
        activeRetryPromise = null;
    });

    return activeRetryPromise;
};


export const authConfig: AuthConfig = {
    authority: '/',
    client_id: 'klabis-web',
    redirect_uri: '/auth/callback', // must match OIDC config
    post_logout_redirect_uri: window.location.origin,
    response_type: 'code',
    scope: 'openid profile MEMBERS EVENTS'
};

export const createUserManager = ({
                                      onUserLoaded = () => {
                                      },
                                      onUserUnloaded = () => {
                                      },
                                      ...config
                                  }: AuthConfig): UserManager => {

    const userManagerConfig = {
        ...config,
        authority: normalizeUrl(config.authority),
        response_type: config.response_type ?? 'code',
        scope: config.scope ?? 'openid profile email',
        userStore: new WebStorageStateStore({store: window.sessionStorage}),
        automaticSilentRenew: true,
        redirect_uri: normalizeUrl(config.redirect_uri),
        post_logout_redirect_uri: normalizeUrl(config.post_logout_redirect_uri),
        silent_redirect_uri: `${window.location.origin}/silent-renew.html`,
    };

    const userManager = new UserManager(userManagerConfig);

    userManager.events.addUserLoaded(onUserLoaded);

    userManager.events.addUserUnloaded(() => {
        console.log('User unloaded');
        onUserUnloaded();
    });

    userManager.events.addAccessTokenExpired(() => {
        console.warn('Access token expired');
        silentRenewRetry(userManager).catch(() => {
            // removeUser() already called inside silentRenewRetry after exhausting all attempts
        });
    });

    userManager.events.addSilentRenewError((err) => {
        // automaticSilentRenew fired and failed — disable it before retrying manually
        // to prevent oidc-client-ts from stacking its own retries on top of ours
        console.error('Silent renew error (automatic):', err);
        userManager.stopSilentRenew();
        silentRenewRetry(userManager)
            .then(() => {
                userManager.startSilentRenew();
            })
            .catch(() => {
                // removeUser() already called inside silentRenewRetry
            });
    });

    userManager.events.addUserSignedOut(() => {
        console.log('User signed out');
        userManager.removeUser();
    });

    return userManager;
};


export const klabisAuthUserManager: UserManager = createUserManager(authConfig);