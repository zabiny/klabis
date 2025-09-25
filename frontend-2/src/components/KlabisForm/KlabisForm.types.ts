export interface KlabisFormProperties<T> {
    formData: T;
    onSubmit: (formData: T) => Promise<void>;
}

export interface KlabisApiFormProperties {
    apiPath: string;
    form: React.ComponentType<KlabisFormProperties<any>>;
    onSuccess?: () => void;
}