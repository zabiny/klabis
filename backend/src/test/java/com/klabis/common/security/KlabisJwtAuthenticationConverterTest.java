package com.klabis.common.security;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.authorization.AuthorizationSnapshotProvider;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for KlabisJwtAuthenticationConverter.
 */
class KlabisJwtAuthenticationConverterTest {

    private static final UUID TEST_USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID TEST_MEMBER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final String TEST_USERNAME = "123456";
    private static final List<String> AUTHORITIES = List.of("MEMBERS:READ", "MEMBERS:WRITE");

    private final AuthorizationSnapshotProvider snapshotProvider = mock(AuthorizationSnapshotProvider.class);
    private final ObjectProvider<AuthorizationSnapshotProvider> snapshotProviderBean = providerOf(snapshotProvider);
    private final KlabisJwtAuthenticationConverter converter = new KlabisJwtAuthenticationConverter(snapshotProviderBean);

    @SuppressWarnings("unchecked")
    private static ObjectProvider<AuthorizationSnapshotProvider> providerOf(AuthorizationSnapshotProvider provider) {
        ObjectProvider<AuthorizationSnapshotProvider> objectProvider = mock(ObjectProvider.class);
        when(objectProvider.getObject()).thenReturn(provider);
        return objectProvider;
    }

    @Test
    @DisplayName("should convert JWT with UserId only")
    void shouldConvertJwtWithUserIdOnly() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString(),
                "authorities", AUTHORITIES
        ));

        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getUserId()).isEqualTo(new UserId(TEST_USER_ID));
        assertThat(token.getMemberIdUuid()).isEmpty();
        assertThat(token.hasMemberProfile()).isFalse();
        assertThat(token.getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("should ignore authorities claim of a user token and take authorities from the request snapshot")
    void shouldTakeUserAuthoritiesFromSnapshotNotFromClaim() {
        when(snapshotProvider.current()).thenReturn(
                AuthorizationSnapshot.of(Set.of(Authority.EVENTS_MANAGE, Authority.GROUPS_TRAINING), Map.of()));
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString(),
                "authorities", AUTHORITIES
        ));

        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) converter.convert(jwt);

        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("EVENTS:MANAGE", "GROUPS:TRAINING");
    }

    @Test
    @DisplayName("should reflect a changed snapshot on the next authorities lookup of the same token")
    void shouldReadSnapshotOnEveryLookup() {
        when(snapshotProvider.current())
                .thenReturn(AuthorizationSnapshot.of(Set.of(Authority.GROUPS_TRAINING), Map.of()))
                .thenReturn(AuthorizationSnapshot.empty());
        Jwt jwt = createTestJwt(Map.of(JwtClaimNames.SUB, TEST_USERNAME, "user_id", TEST_USER_ID.toString()));

        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) converter.convert(jwt);

        assertThat(token.getAuthorities()).hasSize(1);
        assertThat(token.getAuthorities()).isEmpty();
        assertThat(token.hasAuthority(Authority.GROUPS_TRAINING)).isFalse();
    }

    @Test
    @DisplayName("should grant no authorities to a user token outside of a request")
    void shouldGrantNothingOutsideOfRequest() {
        when(snapshotProvider.current()).thenThrow(new ScopeNotActiveException("request", "snapshot", new IllegalStateException()));
        Jwt jwt = createTestJwt(Map.of(JwtClaimNames.SUB, TEST_USERNAME, "user_id", TEST_USER_ID.toString()));

        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) converter.convert(jwt);

        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("should not touch the snapshot while converting a token")
    void shouldNotLoadSnapshotOnConversion() {
        Jwt jwt = createTestJwt(Map.of(JwtClaimNames.SUB, TEST_USERNAME, "user_id", TEST_USER_ID.toString()));

        converter.convert(jwt);

        org.mockito.Mockito.verifyNoInteractions(snapshotProvider);
    }

    @Test
    @DisplayName("should convert JWT with UserId and MemberId")
    void shouldConvertJwtWithUserIdAndMemberId() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString(),
                "member_id", TEST_MEMBER_ID.toString(),
                "authorities", AUTHORITIES
        ));

        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) converter.convert(jwt);

        assertThat(token).isNotNull();
        assertThat(token.getUserId()).isEqualTo(new UserId(TEST_USER_ID));
        assertThat(token.getMemberIdUuid()).isPresent()
                .hasValue(TEST_MEMBER_ID);
        assertThat(token.hasMemberProfile()).isTrue();
    }

    @Test
    @DisplayName("should return plain JwtAuthenticationToken when user_id claim is missing (client credentials token)")
    void shouldReturnPlainJwtAuthenticationTokenWhenUserIdClaimMissing() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, "klabis-frontend",
                "authorities", AUTHORITIES
        ));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull()
                .isNotInstanceOf(KlabisJwtAuthenticationToken.class)
                .isExactlyInstanceOf(JwtAuthenticationToken.class);
        assertThat(token.getAuthorities()).hasSize(2);
    }

    @Test
    @DisplayName("should return plain JwtAuthenticationToken for client credentials token with scope claim")
    void shouldReturnPlainJwtAuthenticationTokenForClientCredentialsToken() {
        converter.setAuthoritiesClaimName("scope");

        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, "automation-client",
                "scope", List.of("MEMBERS:READ")
        ));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token).isNotNull()
                .isNotInstanceOf(KlabisJwtAuthenticationToken.class)
                .isExactlyInstanceOf(JwtAuthenticationToken.class);
        assertThat(token.getAuthorities()).hasSize(1);
    }

    @Test
    @DisplayName("should throw exception when UserId claim is empty")
    void shouldThrowExceptionWhenUserIdClaimIsEmpty() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", "",
                "authorities", AUTHORITIES
        ));

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user_id");
    }

    @Test
    @DisplayName("should throw exception when UserId claim is invalid UUID")
    void shouldThrowExceptionWhenUserIdClaimIsInvalidUuid() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", "not-a-valid-uuid",
                "authorities", AUTHORITIES
        ));

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user_id")
                .hasMessageContaining("not a valid UUID");
    }

    @Test
    @DisplayName("should throw exception when MemberId claim is invalid UUID")
    void shouldThrowExceptionWhenMemberIdClaimIsInvalidUuid() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString(),
                "member_id", "not-a-valid-uuid",
                "authorities", AUTHORITIES
        ));

        assertThatThrownBy(() -> converter.convert(jwt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("member_id")
                .hasMessageContaining("not a valid UUID");
    }

    @Test
    @DisplayName("should convert JWT without MemberId")
    void shouldConvertJwtWithoutMemberId() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString(),
                "authorities", AUTHORITIES
        ));

        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) converter.convert(jwt);

        assertThat(token.getMemberIdUuid()).isEmpty();
    }

    @Test
    @DisplayName("should convert JWT with empty MemberId")
    void shouldConvertJwtWithEmptyMemberId() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString(),
                "member_id", "",
                "authorities", AUTHORITIES
        ));

        KlabisJwtAuthenticationToken token = (KlabisJwtAuthenticationToken) converter.convert(jwt);

        assertThat(token.getMemberIdUuid()).isEmpty();
    }

    @Test
    @DisplayName("should use custom authorities claim name")
    void shouldUseCustomAuthoritiesClaimName() {
        converter.setAuthoritiesClaimName("scopes");

        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, "automation-client",
                "scopes", AUTHORITIES
        ));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).hasSize(2);
    }

    @Test
    @DisplayName("should use custom authority prefix")
    void shouldUseCustomAuthorityPrefix() {
        converter.setAuthorityPrefix("ROLE_");

        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, "automation-client",
                "authorities", List.of("ADMIN")
        ));

        JwtAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getAuthorities()).hasSize(1);
        assertThat(token.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    private Jwt createTestJwt(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", JwsAlgorithms.RS256)
                .claims(claimBuilder -> claimBuilder.putAll(claims))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
