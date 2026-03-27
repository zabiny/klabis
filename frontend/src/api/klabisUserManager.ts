import {User, UserManager, WebStorageStateStore,} from 'oidc-client-ts';
import {normalizeUrl} from "./hateoas.ts";

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
        silent_redirect_uri: normalizeUrl(config.redirect_uri), // Required for silent renew
    };

    const userManager = new UserManager(userManagerConfig);

    userManager.events.addUserLoaded(onUserLoaded);

    userManager.events.addUserUnloaded(() => {
        console.log('User unloaded');
        onUserUnloaded();
    });

    userManager.events.addAccessTokenExpired(() => {
        console.warn('Access token expired');
        userManager.signinSilent().catch((err) => {
            console.error('Silent renew failed', err);
            userManager.removeUser();
        });
    });

    userManager.events.addSilentRenewError((err) => {
        console.error('Silent renew error:', err);
        userManager.removeUser();
    });

    userManager.events.addUserSignedOut(() => {
        console.log('User signed out');
        userManager.removeUser();
    });

    return userManager;
};


export const klabisAuthUserManager: UserManager = createUserManager(authConfig);