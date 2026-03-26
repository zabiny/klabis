package com.klabis.common.security.fieldsecurity;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.BeanSerializerModifier;
import com.klabis.common.users.HasAuthority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Jackson {@link BeanSerializerModifier} that wraps {@link BeanPropertyWriter} instances
 * for record components annotated with {@link PreAuthorize}, {@link HasAuthority}, or
 * {@link OwnerVisible}. Authorization is evaluated during serialization — no interface or
 * proxy needed.
 * <p>
 * Uses {@link ObjectProvider} to allow lazy resolution of {@link OwnershipResolver} and
 * {@link ConversionService}, breaking the circular dependency that arises from Jackson
 * initialization ordering.
 */
class FieldSecurityBeanSerializerModifier extends BeanSerializerModifier {

    private static final Logger log = LoggerFactory.getLogger(FieldSecurityBeanSerializerModifier.class);

    private final ObjectProvider<OwnershipResolver> ownershipResolverProvider;
    private final ObjectProvider<ConversionService> conversionServiceProvider;

    FieldSecurityBeanSerializerModifier(
            ObjectProvider<OwnershipResolver> ownershipResolverProvider,
            ObjectProvider<ConversionService> conversionServiceProvider) {
        this.ownershipResolverProvider = ownershipResolverProvider;
        this.conversionServiceProvider = conversionServiceProvider;
    }

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
        boolean anyOwnerVisible = hasAnyOwnerVisible(recordComponents);

        ConversionService conversionService = anyOwnerVisible ? conversionServiceProvider.getIfAvailable() : null;
        OwnershipResolver ownershipResolver = anyOwnerVisible ? ownershipResolverProvider.getIfAvailable() : null;
        Method ownerIdAccessor = anyOwnerVisible ? resolveOwnerIdAccessor(beanClass, recordComponents, conversionService) : null;

        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter writer = beanProperties.get(i);
            RecordComponent component = findMatchingComponent(writer.getName(), recordComponents);
            if (component == null) {
                continue;
            }

            Method accessor = component.getAccessor();
            PreAuthorize preAuthorize = accessor.getAnnotation(PreAuthorize.class);
            HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);
            boolean ownerVisible = accessor.getAnnotation(OwnerVisible.class) != null;

            if (preAuthorize == null && hasAuthority == null && !ownerVisible) {
                continue;
            }

            HandleAuthorizationDenied deniedHandler = resolveDeniedHandler(accessor, beanClass);
            beanProperties.set(i, new SecuredBeanPropertyWriter(
                    writer, preAuthorize, hasAuthority, deniedHandler, accessor,
                    ownerVisible, ownerIdAccessor, ownershipResolver));
        }

        return beanProperties;
    }

    private boolean hasAnyOwnerVisible(RecordComponent[] components) {
        for (RecordComponent component : components) {
            if (component.getAccessor().getAnnotation(OwnerVisible.class) != null) {
                return true;
            }
        }
        return false;
    }

    private Method resolveOwnerIdAccessor(Class<?> beanClass, RecordComponent[] components, ConversionService conversionService) {
        // Prefer explicit @OwnerId annotation
        for (RecordComponent component : components) {
            if (component.getAccessor().getAnnotation(OwnerId.class) != null) {
                return component.getAccessor();
            }
        }

        if (conversionService == null) {
            log.warn("Cannot resolve owner ID accessor for {}: ConversionService not available.", beanClass.getSimpleName());
            return null;
        }

        // Fall back to single component whose type can be converted to UUID
        List<RecordComponent> candidates = Arrays.stream(components)
                .filter(c -> conversionService.canConvert(c.getType(), UUID.class))
                .toList();

        if (candidates.size() == 1) {
            return candidates.get(0).getAccessor();
        }

        log.warn("Cannot resolve owner ID accessor for {}: found {} UUID-convertible candidates. "
                + "Annotate the owner field with @OwnerId.", beanClass.getSimpleName(), candidates.size());
        return null;
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
