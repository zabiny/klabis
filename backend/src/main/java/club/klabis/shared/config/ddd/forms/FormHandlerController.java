package club.klabis.shared.config.ddd.forms;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.converter.ResolvedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
class FormHandlerController<T> {

    private final FormApiDescriptor<T> formDescription;
    private final ObjectMapper objectMapper;

    public FormHandlerController(FormApiDescriptor<T> formDescription, ObjectMapper objectMapper) {
        this.formDescription = formDescription;
        this.objectMapper = objectMapper;
    }

    @GetMapping(produces = "application/json")
    public T getFormData() {
        return formDescription.formHandler().fetchFormData();
    }

    @GetMapping(produces = "application/schema+json")
    public Schema<?> getFormDataSchema() {
        ResolvedSchema resolved = ModelConverters.getInstance()
                .readAllAsResolvedSchema(formDescription.formType());

        return resolved.schema;
    }

    public void submitFormData(@RequestBody String formData) throws InvalidFormDataException, JsonProcessingException {
        T parsedFormData = objectMapper.readValue(formData, formDescription.formType());

        formDescription.formHandler().submitFormData(parsedFormData);
    }
}
