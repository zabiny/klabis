package club.klabis.adapters.api;

import club.klabis.shared.config.hateoas.HateoasConfig;
import club.klabis.shared.config.restapi.ApisConfiguration;
import club.klabis.shared.config.restapi.KlabisPrincipalSource;
import club.klabis.tests.common.MapperTestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: find way how to not reference KLabisRequestContextImpl here
@Import({MapperTestConfiguration.class, HateoasConfig.class, ApisConfiguration.class})
@MockitoBean(types = {JwtDecoder.class, KlabisPrincipalSource.class})
@WebMvcTest
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiTestConfiguration {
    @AliasFor(annotation = WebMvcTest.class)
    Class<?>[] controllers();
}
