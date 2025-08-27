package club.klabis.shared.config.ddd.forms;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@Validated
class FormHandlerController<T> {

    private final FormHandler<T> formHandler;

    public FormHandlerController(FormHandler<T> formHandler) {
        this.formHandler = formHandler;
    }

    @GetMapping(produces = "application/json")
    public T getFormData() {
        return formHandler.fetchFormData();
    }

    @GetMapping(produces = "application/schema+json")
    public String getFormDataSchema() {
        return "TBD - JSON schema";
    }

    public void submitFormData(@RequestBody @Valid T formData) throws InvalidFormDataException {
        formHandler.submitFormData(formData);
    }
}
