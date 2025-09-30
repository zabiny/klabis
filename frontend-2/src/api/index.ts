import {klabisOpenapiQueryClient} from "./setup";
import type {paths} from "./klabisApi.d.ts";
import type {PathsWithMethod} from "openapi-typescript-helpers";

export * from './types';

const client = klabisOpenapiQueryClient;

export const useKlabisApiQuery = client.useQuery;
export const useKlabisApiMutation = client.useMutation;


export type KlabisApiGetPaths = PathsWithMethod<paths, "get">;
export type KlabisApiPostPaths = PathsWithMethod<paths, "post">;
export type KlabisApiPutPaths = PathsWithMethod<paths, "put">;
export type KlabisApiDeletePaths = PathsWithMethod<paths, "delete">;
export type KlabisApiMutationPaths = KlabisApiPostPaths | KlabisApiDeletePaths | KlabisApiPutPaths;
export type KlabisApiPaths = KlabisApiGetPaths | KlabisApiMutationPaths;