import {User, UserManager, WebStorageStateStore,} from 'oidc-client-ts';
import {normalizeUrl} from "./hateoas.ts";

export interface AuthConfig {
    authority: string;
    client_id: string;
    client_secret?: string;
    redirect_uri: string;
    post_logout_redirect_uri: string;
    response_type?: string;
    scope?: string;
    onUserLoaded?: (user: User) => void;
    onUserUnloaded?: () => void;
    onAuthorizationCompleted?: () => void;
}


export const authConfig: AuthConfig = {
    authority: '/',
    //authority: '/api',
    client_id: 'frontend',
    client_secret: 'fesecret',
    redirect_uri: '/auth/callback', // must match OIDC config
    //redirect_uri: 'http://localhost:3000/auth/callback', // must match OIDC config
    post_logout_redirect_uri: '/oauth/logout',
    response_type: 'code',
    scope: 'openid profile email'
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
        silent_redirect_uri: normalizeUrl(config.redirect_uri), // Required for silent renew
    };

    //console.log(JSON.stringify(userManagerConfig, null, 2))

    const userManager = new UserManager(userManagerConfig);

    userManager.events.addUserLoaded((user) => {
        console.log(`User loaded: ${user}`);
        onUserLoaded(user);
    });

    userManager.events.addUserUnloaded(() => {
        console.log(`User unloaded`);
        onUserUnloaded();
    });

    userManager.events.addAccessTokenExpired(() => {
        console.warn('Access token expired');
        userManager.signinSilent().catch((err) => {
            console.error('Silent renew failed', err);
            //authUserManager.signoutRedirect();
        });
    });

    userManager.events.addSilentRenewError((err) => {
        console.error('Silent renew error:', err);
    });

    userManager.events.addUserSignedOut(() => {
        console.log('User signed out');
        //authUserManager.signoutRedirect();
    });

    return userManager;
};


export const klabisAuthUserManager: UserManager = createUserManager(authConfig);