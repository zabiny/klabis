package club.klabis.shared.config.authserver;

import club.klabis.shared.config.authserver.generatejwtkeys.JKWKeyGenerator;
import club.klabis.shared.config.restapi.KlabisPrincipalSource;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

import java.io.IOException;
import java.text.ParseException;

@Configuration(proxyBeanMethods = false)
public class TokenConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(TokenConfiguration.class);

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws IOException, ParseException {
        JWKSet jwkSet = JWKSet.load(new ClassPathResource(JKWKeyGenerator.AUTH_SERVER_JWK_KEYS_RESOURCE_PATH).getInputStream());
        //JWKSet jwkSet = loadFromFile();
        LOG.debug(jwkSet.toString());
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(KlabisPrincipalSource klabisPrincipalSource) {
        return (context) -> {
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                klabisPrincipalSource.getPrincipalForUserName(context.getPrincipal().getName())
                        .ifPresent(klabisPrincipal -> {
                            context.getClaims()
                                    .claim(StandardClaimNames.PREFERRED_USERNAME, klabisPrincipal.userName());
                            context.getClaims().claim(StandardClaimNames.SUB, klabisPrincipal.userId().value());
                            context.getClaims().claim(StandardClaimNames.GIVEN_NAME, klabisPrincipal.firstName());
                            context.getClaims().claim(StandardClaimNames.FAMILY_NAME, klabisPrincipal.lastName());
                        });
            }
        };
    }

}