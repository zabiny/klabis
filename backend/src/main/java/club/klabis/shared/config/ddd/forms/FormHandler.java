package club.klabis.shared.config.ddd.forms;

public interface FormHandler<T> {
    public T fetchFormData();

    public void submitFormData(T formData) throws InvalidFormDataException;

}
