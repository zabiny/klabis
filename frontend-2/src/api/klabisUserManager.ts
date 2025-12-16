import {type AuthConfig, createUserManager} from "../contexts/auth";
import {UserManager} from "oidc-client-ts";

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

export const klabisAuthUserManager: UserManager = createUserManager(authConfig);