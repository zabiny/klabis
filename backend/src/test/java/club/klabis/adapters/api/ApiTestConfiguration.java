package club.klabis.adapters.api;

import club.klabis.domain.appusers.KlabisApplicationUserDetailsService;
import club.klabis.tests.common.MapperTestConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@TestConfiguration
@Import({ApisConfiguration.class, MapperTestConfiguration.class})
public class ApiTestConfiguration {
    @MockitoBean
    JwtDecoder jwtDecoderMock;

    @MockitoBean
    KlabisSecurityService klabisSecurityServiceMock;

    @MockitoBean
    KlabisApplicationUserDetailsService klabisApplicationUserDetailsServiceMock;
}
