import {type AuthConfig, createUserManager} from "../contexts/auth";
import {UserManager} from "oidc-client-ts";

const authConfig: AuthConfig = {
    authority: 'http://localhost:3000/api',
    client_id: 'frontend',
    client_secret: 'fesecret',
    redirect_uri: 'http://localhost:3000/auth/callback', // must match OIDC config
    post_logout_redirect_uri: 'http://localhost:8080/oauth/logout',
    response_type: 'code',
    scope: 'openid profile email'
};

export const klabisAuthUserManager: UserManager = createUserManager(authConfig);