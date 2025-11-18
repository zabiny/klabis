package club.klabis.shared.config.restapi;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.annotation.AliasFor;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Validated
@Tag(name = "Klabis")
@SecurityRequirement(name = "klabis")
@RestController
@RequestMapping
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ApiController {
    @AliasFor(annotation = Tag.class, attribute = "name")
    String openApiTagName();

    @AliasFor(annotation = SecurityRequirement.class, attribute = "scopes")
    String[] securityScopes() default {"klabis"};

    @AliasFor(annotation = RequestMapping.class, attribute = "value")
    String path() default "";

    @AliasFor(annotation = RequestMapping.class, attribute = "produces")
    String[] produces() default {MediaType.APPLICATION_JSON_VALUE, "application/klabis+json", MediaTypes.HAL_JSON_VALUE, MediaTypes.HAL_FORMS_JSON_VALUE};

}
