package com.klabis.common.security;

import com.klabis.common.authorization.AuthorizationSnapshot;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for KlabisJwtAuthenticationToken.
 */
class KlabisJwtAuthenticationTokenTest {

    private static final UUID TEST_USER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final UUID TEST_MEMBER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final String TEST_USERNAME = "123456";

    @Test
    @DisplayName("should create token with UserId only")
    void shouldCreateTokenWithUserIdOnly() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString()
        ));

        KlabisJwtAuthenticationToken token = new KlabisJwtAuthenticationToken(
                jwt,
                new UserId(TEST_USER_ID),
                null,
                AuthorizationSnapshot::empty
        );

        assertThat(token.getUserId()).isEqualTo(new UserId(TEST_USER_ID));
        assertThat(token.getMemberIdUuid()).isEmpty();
        assertThat(token.hasMemberProfile()).isFalse();
        assertThat(token.getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("should create token with UserId and MemberId")
    void shouldCreateTokenWithUserIdAndMemberId() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString(),
                "member_id", TEST_MEMBER_ID.toString()
        ));

        KlabisJwtAuthenticationToken token = new KlabisJwtAuthenticationToken(
                jwt,
                new UserId(TEST_USER_ID),
                TEST_MEMBER_ID,
                AuthorizationSnapshot::empty
        );

        assertThat(token.getUserId()).isEqualTo(new UserId(TEST_USER_ID));
        assertThat(token.getMemberIdUuid()).isPresent()
                .hasValue(TEST_MEMBER_ID);
        assertThat(token.hasMemberProfile()).isTrue();
        assertThat(token.getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    @DisplayName("should create token with MemberId null")
    void shouldCreateTokenWithMemberIdNull() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString()
        ));

        KlabisJwtAuthenticationToken token = new KlabisJwtAuthenticationToken(
                jwt,
                new UserId(TEST_USER_ID),
                null,
                AuthorizationSnapshot::empty
        );

        assertThat(token.getUserId()).isEqualTo(new UserId(TEST_USER_ID));
        assertThat(token.getMemberIdUuid()).isEmpty();
        assertThat(token.hasMemberProfile()).isFalse();
    }

    @Test
    @DisplayName("should extend JwtAuthenticationToken")
    void shouldExtendJwtAuthenticationToken() {
        Jwt jwt = createTestJwt(Map.of(
                JwtClaimNames.SUB, TEST_USERNAME,
                "user_id", TEST_USER_ID.toString()
        ));

        KlabisJwtAuthenticationToken token = new KlabisJwtAuthenticationToken(
                jwt,
                new UserId(TEST_USER_ID),
                null,
                AuthorizationSnapshot::empty
        );

        assertThat(token.getToken()).isEqualTo(jwt);
        assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    @DisplayName("should identify itself by user id and by member id")
    void shouldIdentifySelfByUserAndMemberId() {
        Jwt jwt = createTestJwt(Map.of(JwtClaimNames.SUB, TEST_USERNAME, "user_id", TEST_USER_ID.toString()));
        UUID stranger = UUID.randomUUID();

        KlabisJwtAuthenticationToken withProfile = new KlabisJwtAuthenticationToken(
                jwt, new UserId(TEST_USER_ID), TEST_MEMBER_ID, AuthorizationSnapshot::empty);
        KlabisJwtAuthenticationToken withoutProfile = new KlabisJwtAuthenticationToken(
                jwt, new UserId(TEST_USER_ID), null, AuthorizationSnapshot::empty);

        assertThat(withProfile.isSelf(TEST_USER_ID)).isTrue();
        assertThat(withProfile.isSelf(TEST_MEMBER_ID)).isTrue();
        assertThat(withProfile.isSelf(stranger)).isFalse();
        assertThat(withoutProfile.isSelf(TEST_USER_ID)).isTrue();
        assertThat(withoutProfile.isSelf(TEST_MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("should expose the overAll authorities of its snapshot as granted authorities")
    void shouldExposeSnapshotAuthoritiesAsGrantedAuthorities() {
        Jwt jwt = createTestJwt(Map.of(JwtClaimNames.SUB, TEST_USERNAME, "user_id", TEST_USER_ID.toString()));
        AuthorizationSnapshot snapshot = AuthorizationSnapshot.of(Set.of(Authority.MEMBERS_READ), Map.of());

        KlabisJwtAuthenticationToken token = new KlabisJwtAuthenticationToken(
                jwt, new UserId(TEST_USER_ID), null, () -> snapshot);

        assertThat(token.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("MEMBERS:READ");
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
