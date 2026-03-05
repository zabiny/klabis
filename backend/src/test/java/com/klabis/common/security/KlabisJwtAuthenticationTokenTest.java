package com.klabis.common.security;

import com.klabis.common.users.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.JwsAlgorithms;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
                List.of()
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
                List.of()
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
                (UUID) null,
                List.of()
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
                List.of()
        );

        assertThat(token.getToken()).isEqualTo(jwt);
        assertThat(token.getAuthorities()).isEmpty();
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
