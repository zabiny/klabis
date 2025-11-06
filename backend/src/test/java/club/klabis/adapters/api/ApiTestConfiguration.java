package club.klabis.adapters.api;

import club.klabis.shared.config.hateoas.HateoasConfig;
import club.klabis.shared.config.restapi.ApisConfiguration;
import club.klabis.shared.config.restapi.KlabisPrincipalSource;
import club.klabis.shared.config.security.KlabisSecurityService;
import club.klabis.tests.common.MapperTestConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
@Import({MapperTestConfiguration.class})
@ComponentScan(basePackageClasses = {ApisConfiguration.class, HateoasConfig.class})
public class ApiTestConfiguration {
    @MockBean
    JwtDecoder jwtDecoderMock;

    @MockBean
    KlabisSecurityService klabisSecurityServiceMock;

    @MockBean
    KlabisPrincipalSource klabisPrincipalSourceMock;

//    @MockBean
//    ApplicationUsersRepository applicationUsersRepositoryMock;
}
