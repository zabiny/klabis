package club.klabis.shared.config.ddd.forms;

import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@ResponseBody
@Validated
class FormHandlerController<T> {

    private final FormHandler<T> formHandler;

    public FormHandlerController(FormHandler<T> formHandler) {
        this.formHandler = formHandler;
    }

    public T getFormData() {
        return formHandler.fetchFormData();
    }

    public void submitFormData(@RequestBody @Valid T formData) throws InvalidFormDataException {
        formHandler.submitFormData(formData);
    }
}
