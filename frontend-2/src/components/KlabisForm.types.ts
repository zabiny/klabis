export interface KlabisFormProperties<T> {
    formData: T;
    onSubmit: (formData: T) => Promise<void>;
}