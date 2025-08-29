package club.klabis.shared.config.ddd.forms;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
class FormHandlerControllersFactory {

    private final ObjectMapper objectMapper;

    FormHandlerControllersFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> FormHandlerController<T> createController(FormApiDescriptor<T> formDescriptor) {
        return new FormHandlerController<>(formDescriptor, objectMapper);
    }

}
