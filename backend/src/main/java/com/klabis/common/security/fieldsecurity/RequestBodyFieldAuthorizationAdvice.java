package com.klabis.common.security.fieldsecurity;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.patch.PatchField;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

@MvcComponent
@ControllerAdvice
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
            if (!PatchField.class.isAssignableFrom(component.getType())) {
                continue;
            }

            Method accessor = component.getAccessor();
            accessor.setAccessible(true);
            PatchField<?> fieldValue;
            try {
                fieldValue = (PatchField<?>) accessor.invoke(record);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to read record component: " + component.getName(), e);
            }

            if (fieldValue == null || !fieldValue.isProvided()) {
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
        for (Parameter parameter : parameters) {
            if (parameter.isAnnotationPresent(OwnerId.class) && parameter.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVariable = parameter.getAnnotation(PathVariable.class);
                String name = pathVariable.value().isEmpty() ? pathVariable.name() : pathVariable.value();
                if (name.isEmpty()) {
                    name = parameter.getName();
                }
                return name;
            }
        }
        return null;
    }
}
