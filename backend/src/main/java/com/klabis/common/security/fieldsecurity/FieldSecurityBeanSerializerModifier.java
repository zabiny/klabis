package com.klabis.common.security.fieldsecurity;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.BeanSerializerBuilder;
import tools.jackson.databind.ser.ValueSerializerModifier;
import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.users.HasAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;

/**
 * Jackson {@link ValueSerializerModifier} that wraps {@link BeanPropertyWriter} instances
 * for record components annotated with {@link HasAuthority} or
 * {@link OwnerVisible}. Authorization is evaluated during serialization — no interface or
 * proxy needed.
 * <p>
 * Runs from {@link #updateBuilder}, not {@link #changeProperties}. Jackson applies every
 * registered modifier's {@code changeProperties} first (in registration order — which Spring
 * does not let us control) and only afterwards calls every modifier's {@code updateBuilder}
 * with the fully-resolved property list. A {@code JsonNullable}-typed property (from
 * {@code org.openapitools:jackson-databind-nullable}) is replaced by that library's own
 * {@code changeProperties}, and since {@link BeanPropertyWriter}'s copy constructor copies
 * state rather than composing a delegate, whichever modifier's {@code changeProperties} runs
 * last wins outright — silently discarding a {@link SecuredBeanPropertyWriter} installed by an
 * earlier {@code changeProperties} call. Wrapping in {@code updateBuilder} instead means we
 * always see, and wrap, whatever writer survived that race, regardless of module registration
 * order.
 * <p>
 * Decisions are delegated to {@link AuthorizationEvaluator}, resolved lazily through an {@link ObjectProvider}
 * because it is not yet available while Jackson is being initialized.
 */
class FieldSecurityBeanSerializerModifier extends ValueSerializerModifier {

    private static final Logger log = LoggerFactory.getLogger(FieldSecurityBeanSerializerModifier.class);

    private final ObjectProvider<AuthorizationEvaluator> evaluatorProvider;
    private volatile AuthorizationEvaluator evaluator;

    FieldSecurityBeanSerializerModifier(ObjectProvider<AuthorizationEvaluator> evaluatorProvider) {
        this.evaluatorProvider = evaluatorProvider;
    }

    private AuthorizationEvaluator evaluator() {
        AuthorizationEvaluator resolved = evaluator;
        if (resolved == null) {
            resolved = evaluatorProvider.getIfAvailable();
            if (resolved == null) {
                log.warn("AuthorizationEvaluator not available, secured fields are hidden");
                return null;
            }
            evaluator = resolved;
        }
        return resolved;
    }

    @Override
    public BeanSerializerBuilder updateBuilder(
            SerializationConfig config,
            BeanDescription.Supplier beanDescSupplier,
            BeanSerializerBuilder builder) {

        Class<?> beanClass = beanDescSupplier.get().getBeanClass();
        if (!beanClass.isRecord()) {
            return builder;
        }

        RecordComponent[] recordComponents = beanClass.getRecordComponents();

        List<BeanPropertyWriter> beanProperties = builder.getProperties();
        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter writer = beanProperties.get(i);
            RecordComponent component = findMatchingComponent(writer.getName(), recordComponents);
            if (component == null) {
                continue;
            }

            Method accessor = component.getAccessor();
            HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);
            boolean ownerVisible = accessor.getAnnotation(OwnerVisible.class) != null;

            if (hasAuthority == null && !ownerVisible) {
                continue;
            }

            HandleAuthorizationDenied deniedHandler = resolveDeniedHandler(accessor, beanClass);
            beanProperties.set(i, new SecuredBeanPropertyWriter(writer, accessor, deniedHandler, this::evaluator));
        }
        builder.setProperties(beanProperties);

        return builder;
    }

    private RecordComponent findMatchingComponent(String propertyName, RecordComponent[] components) {
        for (RecordComponent component : components) {
            if (component.getName().equals(propertyName)) {
                return component;
            }
        }
        return null;
    }

    private HandleAuthorizationDenied resolveDeniedHandler(Method accessor, Class<?> recordClass) {
        HandleAuthorizationDenied methodLevel = accessor.getAnnotation(HandleAuthorizationDenied.class);
        if (methodLevel != null) {
            return methodLevel;
        }
        return recordClass.getAnnotation(HandleAuthorizationDenied.class);
    }
}
