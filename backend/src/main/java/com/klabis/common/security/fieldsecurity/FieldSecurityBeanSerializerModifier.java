package com.klabis.common.security.fieldsecurity;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.klabis.common.users.HasAuthority;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.List;

/**
 * Jackson {@link BeanSerializerModifier} that wraps {@link BeanPropertyWriter} instances
 * for record components annotated with {@link PreAuthorize} or {@link HasAuthority}.
 * Authorization is evaluated during serialization.
 * <p>
 * This approach replaces Spring Security's {@code AuthorizationAdvisorProxyFactory} proxy mechanism
 * for response DTOs. The proxy approach requires records to implement an interface (records are final,
 * so CGLIB cannot subclass them — only JDK dynamic proxy via interface works). With this Jackson-based
 * approach, security annotations go directly on record components and no interface is needed.
 */
class FieldSecurityBeanSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(
            SerializationConfig config,
            BeanDescription beanDesc,
            List<BeanPropertyWriter> beanProperties) {

        Class<?> beanClass = beanDesc.getBeanClass();
        if (!beanClass.isRecord()) {
            return beanProperties;
        }

        RecordComponent[] recordComponents = beanClass.getRecordComponents();

        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter writer = beanProperties.get(i);
            RecordComponent component = findMatchingComponent(writer.getName(), recordComponents);
            if (component == null) {
                continue;
            }

            Method accessor = component.getAccessor();
            PreAuthorize preAuthorize = accessor.getAnnotation(PreAuthorize.class);
            HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);

            if (preAuthorize == null && hasAuthority == null) {
                continue;
            }

            HandleAuthorizationDenied deniedHandler = resolveDeniedHandler(accessor, beanClass);
            beanProperties.set(i, new SecuredBeanPropertyWriter(writer, preAuthorize, hasAuthority, deniedHandler, accessor));
        }

        return beanProperties;
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
