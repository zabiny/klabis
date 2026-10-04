package com.klabis.common.security.fieldsecurity;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TargetParameter;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
class RequestBodyFieldAuthorizationAdvice extends RequestBodyAdviceAdapter {

    private final AuthorizationEvaluator evaluator;

    RequestBodyFieldAuthorizationAdvice(AuthorizationEvaluator evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return methodParameter.getParameterType().isRecord();
    }

    @Override
    public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter,
                                Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!(body instanceof Record record)) {
            return body;
        }

        RecordComponent[] components = record.getClass().getRecordComponents();

        TargetRef target = resolveTargetFromPath(parameter.getMethod());

        for (RecordComponent component : components) {
            if (!JsonNullable.class.isAssignableFrom(component.getType())) {
                continue;
            }

            Method accessor = component.getAccessor();
            accessor.setAccessible(true);
            JsonNullable<?> fieldValue;
            try {
                fieldValue = (JsonNullable<?>) accessor.invoke(record);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read record component: " + component.getName(), e);
            }

            // An explicit null is still "present", so submitting `"field": null` for a privileged
            // field must be rejected rather than waved through as if it were absent.
            if (fieldValue == null || !fieldValue.isPresent()) {
                continue;
            }

            checkFieldAuthorization(component, accessor, target);
        }

        return body;
    }

    private void checkFieldAuthorization(RecordComponent component, Method accessor, @Nullable TargetRef target) {
        if (evaluator.canWriteField(accessor, target)) {
            return;
        }
        PreAuthorize preAuthorize = accessor.getAnnotation(PreAuthorize.class);
        HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);
        String requiredAuthority = hasAuthority != null ? Arrays.stream(hasAuthority.value()).map(Authority::getValue).collect(Collectors.joining(" or "))
                : preAuthorize != null ? preAuthorize.value()
                : "@OwnerVisible";
        throw new FieldAuthorizationException(component.getName(), requiredAuthority);
    }

    @Nullable
    private TargetRef resolveTargetFromPath(@Nullable Method handlerMethod) {
        if (handlerMethod == null) {
            return null;
        }

        TargetParameter targetParameter = evaluator.targetParameterOf(handlerMethod, handlerMethod.getDeclaringClass());
        if (targetParameter == null) {
            return null;
        }
        String paramName = findPathVariableName(handlerMethod, targetParameter.index());
        if (paramName == null) {
            return null;
        }

        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }

        @SuppressWarnings("unchecked")
        Map<String, String> uriVariables = (Map<String, String>) requestAttributes.getRequest()
                .getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (uriVariables == null) {
            return null;
        }

        String rawValue = uriVariables.get(paramName);
        if (rawValue == null) {
            return null;
        }

        try {
            return new TargetRef(targetParameter.type(), UUID.fromString(rawValue));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private String findPathVariableName(Method handlerMethod, int parameterIndex) {
        Parameter[] parameters = handlerMethod.getParameters();
        if (parameterIndex >= parameters.length) {
            return null;
        }

        Parameter parameter = parameters[parameterIndex];
        // @PathVariable itself is a method parameter — not inherited from the interface either,
        // so it must still be present directly on the concrete handler method's parameter.
        if (!parameter.isAnnotationPresent(PathVariable.class)) {
            return null;
        }
        PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
        String name = pathVariable.value().isEmpty() ? pathVariable.name() : pathVariable.value();
        if (name.isEmpty()) {
            name = parameter.getName();
        }
        return name;
    }
}
