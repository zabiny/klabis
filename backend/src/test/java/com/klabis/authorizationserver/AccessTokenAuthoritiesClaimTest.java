package com.klabis.authorizationserver;

import com.klabis.common.security.KlabisOAuth2ClaimNames;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Access token authorities claim")
class AccessTokenAuthoritiesClaimTest {

    private final AuthorizationServerConfiguration config = new AuthorizationServerConfiguration(SimpleObjectProvider.empty());

    private JwtClaimsSet.Builder customize(AuthorizationGrantType grantType, Set<String> scopes) {
        RegisteredClient client = RegisteredClient.withId("id")
                .clientId("client")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(grantType)
                .redirectUri("https://localhost:8443/callback")
                .scopes(s -> s.addAll(scopes))
                .build();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder();
        JwtEncodingContext context = JwtEncodingContext.with(JwsHeader.with(
                        org.springframework.security.oauth2.jose.jws.SignatureAlgorithm.RS256), claims)
                .registeredClient(client)
                .principal(new UsernamePasswordAuthenticationToken("ZBM8001", "n/a",
                        AuthorityUtils.createAuthorityList("GROUPS:TRAINING")))
                .authorizationGrantType(grantType)
                .authorizedScopes(scopes)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .build();
        config.jwtCustomizer().customize(context);
        return claims;
    }

    @Test
    @DisplayName("a user token does not carry authorities")
    void userTokenCarriesNoAuthorities() {
        JwtClaimsSet claims = customize(AuthorizationGrantType.AUTHORIZATION_CODE, Set.of("openid")).subject("ZBM8001").build();

        assertThat(claims.getClaims()).doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES);
    }

    @Test
    @DisplayName("a client_credentials token keeps authorities expanded from its scopes")
    void clientCredentialsTokenKeepsScopeAuthorities() {
        JwtClaimsSet claims = customize(AuthorizationGrantType.CLIENT_CREDENTIALS, Set.of("MEMBERS")).subject("client").build();

        assertThat(claims.<Iterable<String>>getClaim(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES))
                .contains("MEMBERS:READ", "MEMBERS:MANAGE", "MEMBERS:PERMISSIONS");
    }
}
