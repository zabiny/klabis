import {type KlabisApiGetPaths, KlabisApiMutationPaths} from "../../api";

export interface KlabisFormProperties<T> {
    formData: T;
    onSubmit: (formData: T) => Promise<void>;
}

export type KlabisFormApiPaths = KlabisApiGetPaths & KlabisApiMutationPaths;

export interface KlabisApiFormProperties {
    apiPath: KlabisFormApiPaths;
    pathParams: Record<string, string | number>,
    form: React.ComponentType<KlabisFormProperties<any>>;
    onSuccess?: () => void;
}