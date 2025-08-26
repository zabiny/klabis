package club.klabis.shared.config.ddd;

public interface FormHandler<T> {
    public T fetchFormData();

    public void submitFormData(T formData) throws InvalidFormDataException;
}
