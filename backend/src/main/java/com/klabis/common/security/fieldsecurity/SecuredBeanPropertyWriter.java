package com.klabis.common.security.fieldsecurity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.klabis.common.users.HasAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

/**
 * Wraps a {@link BeanPropertyWriter} to evaluate security annotations ({@link PreAuthorize}
 * or {@link HasAuthority}) during serialization. When authorization is denied the field is
 * either masked or skipped entirely, depending on the {@link HandleAuthorizationDenied}
 * configuration resolved from the record component or class level.
 */
class SecuredBeanPropertyWriter extends BeanPropertyWriter {

    private final BeanPropertyWriter delegate;
    private final PreAuthorize preAuthorize;
    private final HasAuthority hasAuthority;
    private final HandleAuthorizationDenied deniedHandler;
    private final Method accessorMethod;

    SecuredBeanPropertyWriter(
            BeanPropertyWriter delegate,
            PreAuthorize preAuthorize,
            HasAuthority hasAuthority,
            HandleAuthorizationDenied deniedHandler,
            Method accessorMethod) {
        super(delegate);
        this.delegate = delegate;
        this.preAuthorize = preAuthorize;
        this.hasAuthority = hasAuthority;
        this.deniedHandler = deniedHandler;
        this.accessorMethod = accessorMethod;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        if (isAuthorized()) {
            delegate.serializeAsField(bean, gen, prov);
            return;
        }

        if (shouldMask()) {
            gen.writeFieldName(delegate.getName());
            gen.writeString(MaskDeniedHandler.MASK_VALUE);
        }
    }

    private boolean isAuthorized() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (preAuthorize != null) {
            return SecuritySpelEvaluator.evaluate(preAuthorize.value(), accessorMethod, authentication);
        }

        if (hasAuthority != null) {
            return SecuritySpelEvaluator.hasAuthority(authentication, hasAuthority.value());
        }

        return true;
    }

    private boolean shouldMask() {
        return deniedHandler != null
                && MaskDeniedHandler.class.isAssignableFrom(deniedHandler.handlerClass());
    }
}
