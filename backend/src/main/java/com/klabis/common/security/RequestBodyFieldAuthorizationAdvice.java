package com.klabis.common.security;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.patch.PatchField;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;

@MvcComponent
@ControllerAdvice
class RequestBodyFieldAuthorizationAdvice extends RequestBodyAdviceAdapter {

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

            checkPreAuthorize(component, accessor);
            checkHasAuthority(component, accessor);
        }

        return body;
    }

    private void checkPreAuthorize(RecordComponent component, Method accessor) {
        PreAuthorize preAuthorize = accessor.getAnnotation(PreAuthorize.class);
        if (preAuthorize == null) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!evaluatePreAuthorize(preAuthorize.value(), accessor, authentication)) {
            throw new FieldAuthorizationException(component.getName(), preAuthorize.value());
        }
    }

    private void checkHasAuthority(RecordComponent component, Method accessor) {
        HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);
        if (hasAuthority == null) {
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthorized(authentication, hasAuthority.value())) {
            throw new FieldAuthorizationException(component.getName(), hasAuthority.value().getValue());
        }
    }

    private boolean evaluatePreAuthorize(String expression, Method method, @Nullable Authentication authentication) {
        return SecuritySpelEvaluator.evaluate(expression, method, authentication);
    }

    private boolean isAuthorized(@Nullable Authentication authentication, Authority requiredAuthority) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        String required = requiredAuthority.getValue();
        return authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals(required));
    }
}

