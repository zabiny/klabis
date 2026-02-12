import {klabisOpenapiQueryClient} from "./setup";
import type {components, paths} from "./klabisApi.d.ts";
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

export type MemberRegistrationForm = components["schemas"]["RegisterMemberRequest"];

export type Link = components["schemas"]["Link"];
export {type HttpMethod} from 'openapi-typescript-helpers';

// Re-export specific password setup types from schemas
export type SetPasswordRequest = components["schemas"]["SetPasswordRequest"];
export type PasswordSetupResponse = components["schemas"]["PasswordSetupResponse"];
export type TokenRequestRequest = components["schemas"]["TokenRequestRequest"];
export type TokenRequestResponse = components["schemas"]["TokenRequestResponse"];
export type ValidateTokenResponse = components["schemas"]["ValidateTokenResponse"];
export type ErrorResponse = components["schemas"]["ErrorResponse"];

