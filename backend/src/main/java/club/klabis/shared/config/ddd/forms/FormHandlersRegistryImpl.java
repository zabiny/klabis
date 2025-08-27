package club.klabis.shared.config.ddd.forms;

import org.springframework.stereotype.Service;

import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
class FormHandlersRegistryImpl implements FormHandlersRegistry {

    private final Map<String, FormHandler<?>> handlers;

    FormHandlersRegistryImpl(Map<String, FormHandler<?>> handlers) {
        this.handlers = handlers;
    }

    @Override
    public Collection<FormApiDescriptor<?>> getFormApis() {
        return handlers.entrySet()
                .stream()
                .map(e -> toFormApi(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public Optional<FormApiDescriptor<?>> findFormApiByPath(String apiPath) {
        return getFormApis().stream().filter(a -> Objects.equals(apiPath, a.apiPath())).findAny();
    }

    private <T> FormApiDescriptor<T> toFormApi(String beanName, FormHandler<T> formHandler) {
        String apiPath = "/form/%s".formatted(beanName);
        Class<T> formType = getFormType(formHandler);
        return new FormApiDescriptor<>(apiPath, formHandler, formType);
    }

    private <T, X extends FormHandler<T>> Class<T> getFormType(X formHandler) {
        ParameterizedType parameterizedType = getFormTypeInterface(formHandler);
        Class<T> formType = (Class<T>) parameterizedType.getActualTypeArguments()[0];
        return formType;
    }

    private <T, X extends FormHandler<T>> ParameterizedType getFormTypeInterface(X formHandler) {
        return Stream.of(formHandler.getClass().getGenericInterfaces())
                .map(ParameterizedType.class::cast)
                .filter(p -> p.getTypeName().startsWith(FormHandler.class.getTypeName()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No form type found for instance %s".formatted(
                        formHandler.getClass())));
    }

}
