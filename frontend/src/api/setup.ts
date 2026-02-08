import createFetchClient, {type Middleware} from "openapi-fetch";
import type {paths} from "./klabisApi";
import {klabisAuthUserManager} from "./klabisUserManager";
import createClient from "openapi-react-query";
import {getApiBaseUrl} from "../utils/getApiBaseUrl";


// https://openapi-ts.dev

const fetchClient = createFetchClient<paths>({
    baseUrl: getApiBaseUrl() || "/",
    headers: {
        "Accept": "application/klabis+json,application/json,application/problem+json"
    }
});

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