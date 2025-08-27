package club.klabis.shared.config.ddd.forms;

public record FormApiDescriptor<T>(String apiPath, FormHandler<T> formHandler, Class<T> formType) {
}
