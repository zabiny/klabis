package com.klabis.common.security.fieldsecurity;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.klabis.common.users.HasAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

/**
 * Wraps a {@link BeanPropertyWriter} to evaluate security annotations ({@link PreAuthorize},
 * {@link HasAuthority}, or {@link OwnerVisible}) during serialization. When authorization is
 * denied the field is either masked or skipped entirely, depending on the
 * {@link HandleAuthorizationDenied} configuration resolved from the record component or class level.
 * <p>
 * Authorization logic uses OR semantics: a field is visible if the authority check passes
 * OR the ownership check passes (when {@link OwnerVisible} is present).
 */
class SecuredBeanPropertyWriter extends BeanPropertyWriter {

    private static final Logger log = LoggerFactory.getLogger(SecuredBeanPropertyWriter.class);

    private final BeanPropertyWriter delegate;
    private final PreAuthorize preAuthorize;
    private final HasAuthority hasAuthority;
    private final HandleAuthorizationDenied deniedHandler;
    private final Method accessorMethod;
    private final boolean ownerVisible;
    private final Method ownerIdAccessor;
    private final OwnershipResolver ownershipResolver;

    SecuredBeanPropertyWriter(
            BeanPropertyWriter delegate,
            PreAuthorize preAuthorize,
            HasAuthority hasAuthority,
            HandleAuthorizationDenied deniedHandler,
            Method accessorMethod,
            boolean ownerVisible,
            Method ownerIdAccessor,
            OwnershipResolver ownershipResolver) {
        super(delegate);
        this.delegate = delegate;
        this.preAuthorize = preAuthorize;
        this.hasAuthority = hasAuthority;
        this.deniedHandler = deniedHandler;
        this.accessorMethod = accessorMethod;
        this.ownerVisible = ownerVisible;
        this.ownerIdAccessor = ownerIdAccessor;
        this.ownershipResolver = ownershipResolver;
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen, SerializerProvider prov) throws Exception {
        if (isAuthorized(bean)) {
            delegate.serializeAsField(bean, gen, prov);
            return;
        }

        if (shouldMask()) {
            gen.writeFieldName(delegate.getName());
            gen.writeString(MaskDeniedHandler.MASK_VALUE);
        }
    }

    private boolean isAuthorized(Object bean) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object ownerIdValue = resolveOwnerIdValue(bean);
        return SecuritySpelEvaluator.isFieldAuthorized(
                preAuthorize, hasAuthority, ownerVisible,
                accessorMethod, ownerIdValue, authentication, ownershipResolver);
    }

    private Object resolveOwnerIdValue(Object bean) {
        if (!ownerVisible || ownerIdAccessor == null) {
            return null;
        }
        try {
            return ownerIdAccessor.invoke(bean);
        } catch (Exception e) {
            log.warn("Failed to read owner ID from bean {}", bean.getClass().getSimpleName(), e);
            return null;
        }
    }

    private boolean shouldMask() {
        return deniedHandler != null
                && MaskDeniedHandler.class.isAssignableFrom(deniedHandler.handlerClass());
    }
}
