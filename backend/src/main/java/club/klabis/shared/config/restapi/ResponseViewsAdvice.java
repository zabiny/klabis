package club.klabis.shared.config.restapi;


import club.klabis.KlabisApplication;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import java.util.Arrays;
import java.util.Optional;

@RestControllerAdvice(basePackageClasses = KlabisApplication.class)
public class ResponseViewsAdvice extends AbstractMappingJacksonResponseBodyAdvice {
    // TODO: configuration annotation shall include also OpenAPI annotations so it's nicely documented in generated OpenAPI - see @PageableAsQueryParam

    private static final Logger LOG = LoggerFactory.getLogger(ResponseViewsAdvice.class);

    private static final String VIEW_REQUEST_PARAMETER = "view";

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return super.supports(returnType, converterType) && returnType.hasMethodAnnotation(JsonViewParameter.class);
    }

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {
        if (bodyContainer.getSerializationView() != null) {
            // do nothing if serialization view is already set
            return;
        }

        JsonViewParameter annotation = returnType.getMethodAnnotation(JsonViewParameter.class);
        if (annotation == null) {
            LOG.warn("Misconfiguration - this advice should be applied only when JsonViewParameter is present");
            return;
        }

        getView(request, annotation)
                .ifPresent(bodyContainer::setSerializationView);
    }

    private static Optional<String> getRequestParam(ServerHttpRequest request, String paramName) {

        try {
            String query = request.getURI().getQuery();

            if (query == null) {
                return Optional.empty();
            }

            return Arrays.stream(query.split("&"))
                    .filter(param -> param.startsWith(paramName + "="))
                    .map(param -> param.substring((paramName + "=").length()))
                    .findFirst();
        } catch (Exception e) {
            LOG.warn("Failed to read %s parameter from request: %s".formatted(paramName, e.getMessage()),
                    e);
            return Optional.empty();
        }
    }

    private Optional<? extends Class<?>> getViewType(JsonViewMapping[] mappings, String viewName) {
        return Arrays.stream(mappings)
                .filter(mapping -> viewName.equals(mapping.name()))
                .map(JsonViewMapping::jsonView)
                .findAny();
    }

    private Optional<? extends Class<?>> getView(ServerHttpRequest request, JsonViewParameter definition) {

        String viewName = getRequestParam(request, definition.name()).orElse(definition.defaultValue());

        if (StringUtils.isBlank(viewName)) {
            return Optional.empty();
        }

        try {
            return getViewType(definition.mapping(), viewName);
        } catch (IllegalArgumentException ex) {
            LOG.warn("Failed to read %s parameter from request: %s".formatted(VIEW_REQUEST_PARAMETER, ex.getMessage()),
                    ex);
            return Optional.empty();

        }
    }

}
