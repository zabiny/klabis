package club.klabis.adapters.api;

import club.klabis.tests.common.MapperTestConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

@TestConfiguration
@Import({ApisConfiguration.class, MapperTestConfiguration.class})
public class ApiTestConfiguration {
    @MockBean
    JwtDecoder jwtDecoderMock;

    @MockBean
    KlabisSecurityService klabisSecurityServiceMock;

    @MockBean
    KlabisApplicationUserDetailsService klabisApplicationUserDetailsServiceMock;
}
