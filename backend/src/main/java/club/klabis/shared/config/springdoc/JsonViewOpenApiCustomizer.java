package club.klabis.shared.config.springdoc;


import club.klabis.shared.config.restapi.JsonViewMapping;
import club.klabis.shared.config.restapi.JsonViewParameter;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.GlobalOperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Přidává query parametr definovaný anotací {@link JsonViewParameter}
 * ke všem operacím, které tuto anotaci mají.
 */
@Component
public class JsonViewOpenApiCustomizer implements GlobalOperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.hasMethodAnnotation(JsonViewParameter.class)) {
            operation.addParametersItem(createParameter(handlerMethod.getMethodAnnotation(JsonViewParameter.class)));
        }
        return operation;
    }

    private Parameter createParameter(JsonViewParameter jsonViewParameter) {
        // Build enum values from the JsonViewMapping annotations
        List<String> enumValues = Arrays.stream(jsonViewParameter.mapping())
                .map(JsonViewMapping::name)
                .collect(Collectors.toList());

        // Create schema with enum values
        StringSchema schema = new StringSchema()
                ._enum(enumValues);

        // Build the Parameter object
        Parameter parameter = new Parameter()
                .in("query")
                .name(jsonViewParameter.name())
                .description(jsonViewParameter.description())
                .required(false)
                .schema(schema);

        // If the annotation provides a default value, set it
        if (!jsonViewParameter.defaultValue().isBlank()) {
            parameter.setExample(jsonViewParameter.defaultValue());
        }

        return parameter;
    }

}