import {User, UserManager, WebStorageStateStore,} from 'oidc-client-ts';

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

export const createUserManager = ({
                                      onUserLoaded = () => {
                                      },
                                      onUserUnloaded = () => {
                                      },
                                      ...config
                                  }: AuthConfig): UserManager => {
    const userManager = new UserManager({
        ...config,
        response_type: config.response_type ?? 'code',
        scope: config.scope ?? 'openid profile email',
        userStore: new WebStorageStateStore({store: window.sessionStorage}),
        automaticSilentRenew: true,
        silent_redirect_uri: config.redirect_uri, // Required for silent renew
    });

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