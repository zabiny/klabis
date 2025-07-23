package club.klabis.config.authserver;

import club.klabis.application.users.ApplicationUsersRepository;
import club.klabis.config.authserver.generatejwtkeys.JKWKeyGenerator;
import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.application.members.MembersRepository;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
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

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws IOException, ParseException {
        JWKSet jwkSet = JWKSet.load(new ClassPathResource(JKWKeyGenerator.AUTH_SERVER_JWK_KEYS_RESOURCE_PATH).getInputStream());
        //JWKSet jwkSet = loadFromFile();
        System.out.println(jwkSet.toString());
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(
            ApplicationUsersRepository appusersRepository, MembersRepository membersRepository) {
        return (context) -> {
            if (OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {
                appusersRepository.findByUserName(context.getPrincipal().getName()).flatMap(ApplicationUser::getMemberId).ifPresent(memberId -> {
                    context.getClaims().claim(StandardClaimNames.PREFERRED_USERNAME, context.getPrincipal().getName());
                    context.getClaims().claim(StandardClaimNames.SUB, memberId);
                    membersRepository.findById(memberId).ifPresent(existingMember -> {
                        context.getClaims().claim(StandardClaimNames.GIVEN_NAME, existingMember.getFirstName());
                        context.getClaims().claim(StandardClaimNames.FAMILY_NAME, existingMember.getLastName());
                    });
                });
            }
        };
    }

//    @Bean
//    public OAuth2TokenCustomizer<OAuth2TokenClaimsContext> opaqueTokenCustomizer() {
//        return context -> {
//            UserDetails userDetails = null;
//
//            if (context.getPrincipal() instanceof OAuth2ClientAuthenticationToken) {
//                userDetails = (UserDetails) context.getPrincipal().getDetails();
//            } else if (context.getPrincipal() instanceof AbstractAuthenticationToken) {
//                userDetails = (UserDetails) context.getPrincipal().getPrincipal();
//            } else {
//                throw new IllegalStateException("Unexpected token type");
//            }
//
//            if (!StringUtils.hasText(userDetails.getUsername())) {
//                throw new IllegalStateException("Bad UserDetails, username is empty");
//            }
//
//            context.getClaims()
//                    .claim(
//                            "authorities",
//                            userDetails.getAuthorities().stream()
//                                    .map(GrantedAuthority::getAuthority)
//                                    .collect(Collectors.toSet())
//                    )
//                    .claim(
//                            "username", userDetails.getUsername()
//                    );
//        };
//    }
}