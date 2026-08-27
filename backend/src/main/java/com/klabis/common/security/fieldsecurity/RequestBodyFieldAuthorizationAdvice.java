package com.klabis.common.security.fieldsecurity;

import com.klabis.common.security.MethodSecurityAnnotations;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
class RequestBodyFieldAuthorizationAdvice extends RequestBodyAdviceAdapter {

    private final OwnershipResolver ownershipResolver;

    RequestBodyFieldAuthorizationAdvice(OwnershipResolver ownershipResolver) {
        this.ownershipResolver = ownershipResolver;
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        RecordComponent[] components = record.getClass().getRecordComponents();

        UUID ownerIdFromPath = resolveOwnerIdFromPath(parameter.getMethod());

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

            checkFieldAuthorization(component, accessor, authentication, ownerIdFromPath);
        }

        return body;
    }

    private void checkFieldAuthorization(RecordComponent component, Method accessor,
                                         @Nullable Authentication authentication,
                                         @Nullable UUID ownerIdFromPath) {
        PreAuthorize preAuthorize = accessor.getAnnotation(PreAuthorize.class);
        HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);
        boolean ownerVisible = accessor.getAnnotation(OwnerVisible.class) != null;

        if (preAuthorize == null && hasAuthority == null && !ownerVisible) {
            return;
        }

        if (!SecuritySpelEvaluator.isFieldAuthorized(
                preAuthorize, hasAuthority, ownerVisible,
                accessor, ownerIdFromPath, authentication, ownershipResolver)) {
            String requiredAuthority = hasAuthority != null ? hasAuthority.value().getValue()
                    : preAuthorize != null ? preAuthorize.value()
                    : "@OwnerVisible";
            throw new FieldAuthorizationException(component.getName(), requiredAuthority);
        }
    }

    @Nullable
    private UUID resolveOwnerIdFromPath(@Nullable Method handlerMethod) {
        if (handlerMethod == null) {
            return null;
        }

        String ownerParamName = findOwnerIdParameterName(handlerMethod);
        if (ownerParamName == null) {
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

        String rawValue = uriVariables.get(ownerParamName);
        if (rawValue == null) {
            return null;
        }

        try {
            return UUID.fromString(rawValue);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Nullable
    private String findOwnerIdParameterName(Method handlerMethod) {
        Parameter[] parameters = handlerMethod.getParameters();
        int ownerIdIndex = MethodSecurityAnnotations.findAnnotatedParameterIndex(
                handlerMethod, handlerMethod.getDeclaringClass(), OwnerId.class);
        if (ownerIdIndex < 0 || ownerIdIndex >= parameters.length) {
            return null;
        }

        Parameter parameter = parameters[ownerIdIndex];
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
