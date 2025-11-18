package club.klabis.shared.config.restapi.context;

import club.klabis.members.MemberId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
class ContextHandler implements HandlerInterceptor {
    private final KlabisRequestContextManager context;
    private final HttpServletRequest request;

    public ContextHandler(KlabisRequestContextManager context, HttpServletRequest request) {
        this.context = context;
        this.request = request;
    }

    private Map<String, String> getUriTemplateVariables() {
        Map<String, String> uriTemplateVariables = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        return Objects.requireNonNullElse(uriTemplateVariables, new HashMap<>());
    }

    private Optional<String> getPathVariable(String variableName) {
        return Optional.ofNullable(getUriTemplateVariables().get(variableName));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        getPathVariable("memberId").ifPresent(memberId -> {
            context.setMemberId(new MemberId(Integer.parseInt(memberId)));
        });

        return true;
    }
}
