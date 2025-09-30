import {type AuthConfig, createUserManager} from "../contexts/auth";
import createFetchClient, {type Middleware} from "openapi-fetch";
import type {paths} from "./klabisApi";
import {UserManager} from "oidc-client-ts";
import createClient from "openapi-react-query";

const authConfig: AuthConfig = {
    authority: 'http://localhost:3000/api',
    client_id: 'frontend',
    client_secret: 'fesecret',
    redirect_uri: 'http://localhost:3000/auth/callback', // must match OIDC config
    post_logout_redirect_uri: 'http://localhost:8080/oauth/logout',
    response_type: 'code',
    scope: 'openid profile email'
};

// https://openapi-ts.dev

const fetchClient = createFetchClient<paths>({
    baseUrl: "/api",
    headers: {
        "Accept": "application/klabis+json,application/json,application/problem+json"
    }
});

const klabisAuthUserManager: UserManager = createUserManager(authConfig);

const promiseAccessToken = async (): Promise<string | null> => {
    const authUser = await klabisAuthUserManager.getUser();
    return !authUser?.expired && authUser?.access_token || null;
}

// https://openapi-ts.dev/openapi-fetch/middleware-auth#auth
const authMiddleware: Middleware = {
    async onRequest({request}) {

        const access_token = await promiseAccessToken();

        if (!access_token) {
            throw new Error("No Klabis access_token available!");
        }

        // add Authorization header to every request
        request.headers.set("Authorization", `Bearer ${access_token}`);

        return request;
    },
};

fetchClient.use(authMiddleware);


const client = createClient(fetchClient);

export const klabisOpenapiQueryClient = client;