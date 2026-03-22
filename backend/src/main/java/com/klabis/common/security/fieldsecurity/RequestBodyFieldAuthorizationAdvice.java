package com.klabis.common.security.fieldsecurity;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.patch.PatchField;
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

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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

            checkPreAuthorize(component, accessor, authentication);
            checkHasAuthority(component, accessor, authentication);
        }

        return body;
    }

    private void checkPreAuthorize(RecordComponent component, Method accessor, @Nullable Authentication authentication) {
        PreAuthorize preAuthorize = accessor.getAnnotation(PreAuthorize.class);
        if (preAuthorize == null) {
            return;
        }

        if (!SecuritySpelEvaluator.evaluate(preAuthorize.value(), accessor, authentication)) {
            throw new FieldAuthorizationException(component.getName(), preAuthorize.value());
        }
    }

    private void checkHasAuthority(RecordComponent component, Method accessor, @Nullable Authentication authentication) {
        HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);
        if (hasAuthority == null) {
            return;
        }

        if (!SecuritySpelEvaluator.hasAuthority(authentication, hasAuthority.value())) {
            throw new FieldAuthorizationException(component.getName(), hasAuthority.value().getValue());
        }
    }
}
