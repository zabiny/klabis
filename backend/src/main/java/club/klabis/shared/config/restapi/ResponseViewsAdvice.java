package club.klabis.shared.config.restapi;


import club.klabis.KlabisApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMappingJacksonResponseBodyAdvice;

import java.util.Optional;

@RestControllerAdvice(basePackageClasses = KlabisApplication.class)
public class ResponseViewsAdvice extends AbstractMappingJacksonResponseBodyAdvice {
    // TODO: add configuration (name of request parameter, enum type) - for example @JsonViewParameter(name = "view", type = ResponseViews.class, default = ResponseViews.DETAILED)
    // TODO: configuration annotation shall include also OpenAPI annotations so it's nicely documented in generated OpenAPI - see @PageableAsQueryParam

    private static final Logger LOG = LoggerFactory.getLogger(ResponseViewsAdvice.class);

    private static final String VIEW_REQUEST_PARAMETER = "view";

    @Override
    protected void beforeBodyWriteInternal(MappingJacksonValue bodyContainer, MediaType contentType, MethodParameter returnType, ServerHttpRequest request, ServerHttpResponse response) {
        if (bodyContainer.getSerializationView() != null) {
            // do nothing if serialization view is already set
            return;
        }

        getView(request)
                .map(ResponseViews::getJsonView)
                .ifPresent(bodyContainer::setSerializationView);
    }

    private Optional<ResponseViews> getView(ServerHttpRequest request) {
        try {
            String query = request.getURI().getQuery();

            if (query == null) {
                return Optional.empty();
            }

            String viewParam = java.util.Arrays.stream(query.split("&"))
                    .filter(param -> param.startsWith(VIEW_REQUEST_PARAMETER + "="))
                    .map(param -> param.substring((VIEW_REQUEST_PARAMETER + "=").length()))
                    .findFirst()
                    .orElse(null);

            return Optional.of(ResponseViews.valueOf(viewParam));
        } catch (Exception e) {
            LOG.warn("Failed to read %s parameter from request: %s".formatted(VIEW_REQUEST_PARAMETER, e.getMessage()),
                    e);
            return Optional.empty();
        }
    }

}
