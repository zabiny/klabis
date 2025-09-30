import createFetchClient from "openapi-fetch";
import createClient from "openapi-react-query";
import type {paths} from "./klabisApi.d.ts";
import type {PathsWithMethod} from "openapi-typescript-helpers";

const fetchClient = createFetchClient<paths>({
    baseUrl: "/api",
});

export const useKlabisApiQuery = createClient(fetchClient).useQuery;
export const useKlabisApiMutation = createClient(fetchClient).useMutation;


export type klabisApiGetMethods = PathsWithMethod<paths, "get">;