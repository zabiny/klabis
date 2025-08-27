package club.klabis.shared.config.ddd.forms;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.JsonSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Improves form API endpoints specifications - adds response type to GET API, adds request body to PUT API, etc..
 */
@Service
class FormsOpenApiCustomizer implements GlobalOpenApiCustomizer {

    private static final Logger LOG = LoggerFactory.getLogger(FormsOpenApiCustomizer.class);

    private final FormHandlersRegistry formHandlersRegistry;

    FormsOpenApiCustomizer(FormHandlersRegistry formHandlersRegistry) {
        this.formHandlersRegistry = formHandlersRegistry;
    }

    @Override
    public void customise(OpenAPI openApi) {
        // add form schemas
        getFormSchemas().entrySet().forEach(e -> openApi.getComponents().addSchemas(e.getKey(), e.getValue()));

        // add response body type to GET and request body type to PUT endpoints
        openApi.getPaths().forEach(this::customizePathItem);
    }

    private Map<String, Schema<?>> getFormSchemas() {
        Map<String, Schema<?>> result = new HashMap<>();
        getFormOpenApiDescriptors().stream().forEach(descriptor -> {
            result.put(descriptor.getSchemaName(), descriptor.createOpenApiSchema());
        });
        return result;
    }

    private void customizePathItem(String apiPath, PathItem item) {
        formHandlersRegistry.findFormApiByPath(apiPath)
                .map(FormOpenApiDescriptor::new)
                .ifPresentOrElse(descriptor -> customizePathItem(item, descriptor),
                        () -> LOG.trace("API path %s is not form API"));
    }

    private void customizePathItem(PathItem pathItem, FormOpenApiDescriptor apiDescriptor) {
        pathItem.getGet()
                .description("Returns data to populate form %s".formatted(apiDescriptor.descriptor()
                        .formType()
                        .getCanonicalName()))
                .getResponses()
                .get("200")
                .getContent()
                .addMediaType("application/json",
                        new MediaType().schema(apiDescriptor.createOpenApiSchemaReference()));

        pathItem.getPut()
                .description("Processes submitted data from form %s".formatted(apiDescriptor.descriptor()
                        .formType()
                        .getCanonicalName()))
                .getRequestBody()
                .getContent()
                .addMediaType("application/json",
                        new MediaType().schema(apiDescriptor.createOpenApiSchemaReference()));
    }

    private Collection<FormOpenApiDescriptor> getFormOpenApiDescriptors() {
        return formHandlersRegistry.getFormApis().stream().map(FormOpenApiDescriptor::new).toList();
    }

    record FormOpenApiDescriptor(FormApiDescriptor<?> descriptor) {
        public Schema<?> createOpenApiSchema() {
            return new JsonSchema();
        }

        public Schema<?> createOpenApiSchemaReference() {
            return new Schema().$ref("/components/schemas/%s".formatted(getSchemaName()));
        }

        public String getSchemaName() {
            return descriptor().formType().getSimpleName();
        }
    }
}
