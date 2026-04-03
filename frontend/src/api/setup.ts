import createFetchClient, {type Middleware} from "openapi-fetch";
import type {paths} from "./klabisApi";
import {klabisAuthUserManager, silentRenewRetry} from "./klabisUserManager";
import createClient from "openapi-react-query";


// https://openapi-ts.dev

const fetchClient = createFetchClient<paths>({
    baseUrl: '/api',
    headers: {
        "Accept": "application/klabis+json,application/json,application/problem+json"
    }
});

const promiseAccessToken = async (): Promise<string | null> => {
    const authUser = await klabisAuthUserManager.getUser();
    if (!authUser || authUser.expired || !authUser.access_token) return null;
    return authUser.access_token;
}

// https://openapi-ts.dev/openapi-fetch/middleware-auth#auth
export const authMiddleware: Middleware = {
    async onRequest({request}) {
        const access_token = await promiseAccessToken();

        if (!access_token) {
            throw new Error("No Klabis access_token available!");
        }

        request.headers.set("Authorization", `Bearer ${access_token}`);
        return request;
    },

    async onResponse({request, response}) {
        if (response.status !== 401) {
            return undefined;
        }

        await silentRenewRetry(klabisAuthUserManager);

        // Retry the original request once with the freshly renewed token
        const renewedUser = await klabisAuthUserManager.getUser();
        const retryRequest = new Request(request);
        retryRequest.headers.set("Authorization", `Bearer ${renewedUser?.access_token}`);

        return fetch(retryRequest);
    },
};

fetchClient.use(authMiddleware);


const client = createClient(fetchClient);

export const klabisOpenapiQueryClient = client;
