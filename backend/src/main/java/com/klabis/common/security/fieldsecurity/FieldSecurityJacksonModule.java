package com.klabis.common.security.fieldsecurity;

import com.klabis.common.authorization.AuthorizationEvaluator;
import tools.jackson.databind.module.SimpleModule;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.jackson.JacksonComponent;

/**
 * Jackson module that registers {@link FieldSecurityBeanSerializerModifier} so that
 * record components annotated with security annotations are evaluated at serialization time.
 * Spring Boot auto-discovers this module via {@link JsonComponent}.
 * <p>
 * Uses {@link ObjectProvider} to break the circular dependency that arises when Jackson
 * is initialized before the full MVC context is ready.
 */
@JacksonComponent
class FieldSecurityJacksonModule extends SimpleModule {

    FieldSecurityJacksonModule(ObjectProvider<AuthorizationEvaluator> evaluator) {
        super("FieldSecurityJacksonModule");
        setSerializerModifier(new FieldSecurityBeanSerializerModifier(evaluator));
    }
}
