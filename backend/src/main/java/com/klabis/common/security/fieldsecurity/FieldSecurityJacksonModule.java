package com.klabis.common.security.fieldsecurity;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.boot.jackson.JsonComponent;

/**
 * Jackson module that registers {@link FieldSecurityBeanSerializerModifier} so that
 * record components annotated with security annotations are evaluated at serialization time.
 * Spring Boot auto-discovers this module via {@link JsonComponent}.
 */
@JsonComponent
class FieldSecurityJacksonModule extends SimpleModule {

    FieldSecurityJacksonModule() {
        super("FieldSecurityJacksonModule");
        setSerializerModifier(new FieldSecurityBeanSerializerModifier());
    }
}
