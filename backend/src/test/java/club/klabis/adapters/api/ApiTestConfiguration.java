package club.klabis.adapters.api;

import club.klabis.shared.config.hateoas.HateoasConfig;
import club.klabis.shared.config.restapi.ApisConfiguration;
import club.klabis.shared.config.restapi.KlabisPrincipalSource;
import club.klabis.tests.common.MapperTestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// TODO: find way how to not reference KlabisPrincipalSource here (possibly will need to cleanup whole security setup)
@EnableHypermediaSupport(type = {EnableHypermediaSupport.HypermediaType.HAL, EnableHypermediaSupport.HypermediaType.HAL_FORMS})
@Import({MapperTestConfiguration.class, HateoasConfig.class, ApisConfiguration.class})
@MockitoBean(types = {JwtDecoder.class, KlabisPrincipalSource.class})
@WebMvcTest
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
// Not sure why enabled HATEOAS doesn't include it's repremodelprocessors ...
//@ComponentScan(
//        useDefaultFilters = false,
//        includeFilters = @ComponentScan.Filter(
//                type = FilterType.ASSIGNABLE_TYPE,
//                classes = {RepresentationModelProcessor.class, ModelPreparator.class}
//        )
//)
public @interface ApiTestConfiguration {
    @AliasFor(annotation = WebMvcTest.class)
    Class<?>[] controllers();
}
