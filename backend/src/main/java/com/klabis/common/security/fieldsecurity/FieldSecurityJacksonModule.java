package com.klabis.common.security.fieldsecurity;

import tools.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jackson.JacksonComponent;
import org.springframework.core.convert.ConversionService;

/**
 * Jackson module that registers {@link FieldSecurityBeanSerializerModifier} so that
 * record components annotated with security annotations are evaluated at serialization time.
 * Spring Boot auto-discovers this module via {@link JsonComponent}.
 * <p>
 * Uses {@link ObjectProvider} to break the circular dependency that arises when Jackson
 * is initialized before the full MVC context (including ConversionService) is ready.
 */
@JacksonComponent
class FieldSecurityJacksonModule extends SimpleModule {

    FieldSecurityJacksonModule(ObjectProvider<OwnershipResolver> ownershipResolver, ObjectProvider<ConversionService> conversionService) {
        super("FieldSecurityJacksonModule");
        setSerializerModifier(new FieldSecurityBeanSerializerModifier(ownershipResolver, conversionService));
    }
}
