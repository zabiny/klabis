package club.klabis.adapters.api;

import club.klabis.shared.config.hateoas.HateoasConfig;
import club.klabis.shared.config.restapi.ApisConfiguration;
import club.klabis.shared.config.restapi.KlabisPrincipalSource;
import club.klabis.shared.config.security.KlabisSecurityService;
import club.klabis.tests.common.MapperTestConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestConfiguration
@Import({MapperTestConfiguration.class})
@ComponentScan(basePackageClasses = {ApisConfiguration.class, HateoasConfig.class})
public class ApiTestConfiguration {
    @MockitoBean
    JwtDecoder jwtDecoderMock;

    @MockitoBean
    KlabisSecurityService klabisSecurityServiceMock;

    @MockitoBean
    KlabisPrincipalSource klabisPrincipalSourceMock;

//    @MockBean
//    ApplicationUsersRepository applicationUsersRepositoryMock;
}
