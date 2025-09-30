import {User, UserManager, WebStorageStateStore,} from 'oidc-client-ts';

export interface AuthUserDetails {
    firstName: string,
    lastName: string,
    id: number,
    registrationNumber: string
}

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
}

export const createUserManager = ({
                                      onUserLoaded = (user) => {
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

    // Handle the redirect callback on app load
    if (window.location.pathname === new URL(config.redirect_uri).pathname) {
        userManager
            .signinRedirectCallback()
            .then((user) => {
                console.log('Signin redirect callback success:', user);
                onUserLoaded(user);
                // Clean URL after processing
                window.history.replaceState({}, document.title, '/');
            })
            .catch((err) => {
                console.error('Signin redirect callback error:', err);
            });
    } else {
        // Try to get existing user from storage
        userManager
            .getUser()
            .then((user) => {
                if (user && !user.expired) {
                    onUserLoaded(user);
                }
            })
            .catch(console.error);
    }

    userManager.events.addUserLoaded((user) => {
        onUserLoaded(user);
    });

    userManager.events.addUserUnloaded(() => {
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