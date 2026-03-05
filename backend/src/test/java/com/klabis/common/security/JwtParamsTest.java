package com.klabis.common.security;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.members.CurrentUserData;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtParams Tests")
class JwtParamsTest {

    private static final String TEST_USERNAME = "ZBM8001";
    private static final UUID TEST_USER_ID = UUID.fromString("92093b28-24c0-41d7-a297-38bf33ddff9e");
    private static final UUID TEST_MEMBER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");

    @Nested
    @DisplayName("Static Factory Methods")
    class FactoryMethods {

        @Test
        @DisplayName("should create JwtParams from CurrentUserData without memberId")
        void shouldCreateFromCurrentUserDataWithoutMemberId() {
            CurrentUserData userData = new CurrentUserData(TEST_USERNAME, new UserId(TEST_USER_ID), null);

            JwtParams result = JwtParams.jwtTokenParams(userData);

            assertThat(result.userName()).isEqualTo(TEST_USERNAME);
            assertThat(result.userId().uuid()).isEqualTo(TEST_USER_ID);
            assertThat(result.memberId()).isNull();
        }

        @Test
        @DisplayName("should create JwtParams from CurrentUserData with memberId")
        void shouldCreateFromCurrentUserDataWithMemberId() {
            MemberId memberId = new MemberId(TEST_MEMBER_ID);
            CurrentUserData userData = new CurrentUserData(TEST_USERNAME, new UserId(TEST_USER_ID), memberId);

            JwtParams result = JwtParams.jwtTokenParams(userData);

            assertThat(result.userName()).isEqualTo(TEST_USERNAME);
            assertThat(result.userId().uuid()).isEqualTo(TEST_USER_ID);
            assertThat(result.memberId()).isEqualTo(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("should create JwtParams from userName and userId UUID")
        void shouldCreateFromUserNameAndUserIdUUID() {
            JwtParams result = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);

            assertThat(result.userName()).isEqualTo(TEST_USERNAME);
            assertThat(result.userId().uuid()).isEqualTo(TEST_USER_ID);
            assertThat(result.memberId()).isNull();
        }

        @Test
        @DisplayName("should create JwtParams from userName and UserId object")
        void shouldCreateFromUserNameAndUserIdObject() {
            UserId userId = new UserId(TEST_USER_ID);
            JwtParams result = JwtParams.jwtTokenParams(TEST_USERNAME, userId);

            assertThat(result.userName()).isEqualTo(TEST_USERNAME);
            assertThat(result.userId()).isEqualTo(userId);
            assertThat(result.memberId()).isNull();
        }
    }

    @Nested
    @DisplayName("Fluent API Methods")
    class FluentApi {

        @Test
        @DisplayName("withMemberId(UUID) should add memberId to existing params")
        void withMemberIdUUIDShouldAddMemberId() {
            JwtParams base = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);

            JwtParams result = base.withMemberId(TEST_MEMBER_ID);

            assertThat(result.userName()).isEqualTo(TEST_USERNAME);
            assertThat(result.userId().uuid()).isEqualTo(TEST_USER_ID);
            assertThat(result.memberId()).isEqualTo(TEST_MEMBER_ID);
            assertThat(result.authorities()).isEmpty();
        }

        @Test
        @DisplayName("withMemberId(MemberId) should add memberId to existing params")
        void withMemberIdObjectShouldAddMemberId() {
            JwtParams base = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);
            MemberId memberId = new MemberId(TEST_MEMBER_ID);

            JwtParams result = base.withMemberId(memberId);

            assertThat(result.memberId()).isEqualTo(TEST_MEMBER_ID);
        }

        @Test
        @DisplayName("withMemberId(null) should handle null memberId")
        void withMemberIdNullShouldHandleNull() {
            JwtParams base = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);

            JwtParams result = base.withMemberId((MemberId) null);

            assertThat(result.memberId()).isNull();
        }

        @Test
        @DisplayName("withAuthorities should replace authorities")
        void withAuthoritiesShouldReplaceAuthorities() {
            JwtParams base = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);

            JwtParams result = base.withAuthorities(Authority.MEMBERS_READ, Authority.EVENTS_MANAGE);

            assertThat(result.authorities()).containsExactly(Authority.MEMBERS_READ, Authority.EVENTS_MANAGE);
        }

        @Test
        @DisplayName("addAuthority should append to existing authorities")
        void addAuthorityShouldAppendToExistingAuthorities() {
            JwtParams base = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID)
                    .withAuthorities(Authority.MEMBERS_READ);

            JwtParams result = base.addAuthority(Authority.EVENTS_MANAGE);

            assertThat(result.authorities()).containsExactly(Authority.MEMBERS_READ, Authority.EVENTS_MANAGE);
        }

        @Test
        @DisplayName("addAuthority should work with empty authorities")
        void addAuthorityShouldWorkWithEmptyAuthorities() {
            JwtParams base = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);

            JwtParams result = base.addAuthority(Authority.MEMBERS_CREATE);

            assertThat(result.authorities()).containsExactly(Authority.MEMBERS_CREATE);
        }
    }

    @Nested
    @DisplayName("grantedAuthorities() Conversion")
    class GrantedAuthorities {

        @Test
        @DisplayName("should convert Authority enum to SimpleGrantedAuthority")
        void shouldConvertAuthorityToSimpleGrantedAuthority() {
            JwtParams params = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID)
                    .withAuthorities(Authority.MEMBERS_READ, Authority.EVENTS_MANAGE);

            var result = params.grantedAuthorities();

            assertThat(result).hasSize(2);
            assertThat(result).allMatch(auth -> auth instanceof org.springframework.security.core.authority.SimpleGrantedAuthority);
            assertThat(result.stream().map(org.springframework.security.core.GrantedAuthority::getAuthority))
                    .containsExactlyInAnyOrder("MEMBERS:READ", "EVENTS:MANAGE");
        }

        @Test
        @DisplayName("should return empty collection when no authorities")
        void shouldReturnEmptyCollectionWhenNoAuthorities() {
            JwtParams params = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);

            var result = params.grantedAuthorities();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("toKlabisClaims() Method")
    class ToKlabisClaims {

        @Test
        @DisplayName("should create basic claims without memberId and authorities")
        void shouldCreateBasicClaims() {
            JwtParams params = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);

            Map<String, Object> claims = params.toKlabisClaims();

            assertThat(claims).hasSize(3);
            assertThat(claims.get("sub")).isEqualTo(TEST_USERNAME);
            assertThat(claims.get(KlabisOAuth2ClaimNames.CLAIM_USER_ID)).isEqualTo(TEST_USER_ID.toString());
            assertThat(claims.get(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES)).isInstanceOf(java.util.Collection.class);
            assertThat((java.util.Collection<?>) claims.get(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES)).isEmpty();
        }

        @Test
        @DisplayName("should include memberId when present")
        void shouldIncludeMemberIdWhenPresent() {
            JwtParams params = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID)
                    .withMemberId(TEST_MEMBER_ID);

            Map<String, Object> claims = params.toKlabisClaims();

            assertThat(claims).hasSize(4);
            assertThat(claims.get(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID)).isEqualTo(TEST_MEMBER_ID.toString());
        }

        @Test
        @DisplayName("should include authorities when present")
        void shouldIncludeAuthoritiesWhenPresent() {
            JwtParams params = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID)
                    .withAuthorities(Authority.MEMBERS_READ, Authority.EVENTS_MANAGE);

            Map<String, Object> claims = params.toKlabisClaims();

            assertThat(claims).hasSize(3);
            @SuppressWarnings("unchecked")
            java.util.Collection<String> authorities = (java.util.Collection<String>) claims.get(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES);
            assertThat(authorities).containsExactlyInAnyOrder("MEMBERS:READ", "EVENTS:MANAGE");
        }

        @Test
        @DisplayName("should include all claims when fully populated")
        void shouldIncludeAllClaimsWhenFullyPopulated() {
            JwtParams params = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID)
                    .withMemberId(TEST_MEMBER_ID)
                    .withAuthorities(Authority.MEMBERS_CREATE, Authority.MEMBERS_UPDATE);

            Map<String, Object> claims = params.toKlabisClaims();

            assertThat(claims).hasSize(4);
            assertThat(claims.get("sub")).isEqualTo(TEST_USERNAME);
            assertThat(claims.get(KlabisOAuth2ClaimNames.CLAIM_USER_ID)).isEqualTo(TEST_USER_ID.toString());
            assertThat(claims.get(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID)).isEqualTo(TEST_MEMBER_ID.toString());
            @SuppressWarnings("unchecked")
            java.util.Collection<String> authorities = (java.util.Collection<String>) claims.get(KlabisOAuth2ClaimNames.CLAIM_AUTHORITIES);
            assertThat(authorities).containsExactlyInAnyOrder("MEMBERS:CREATE", "MEMBERS:UPDATE");
        }

        @Test
        @DisplayName("should return independent map on each call")
        void shouldReturnIndependentMapOnEachCall() {
            JwtParams params = JwtParams.jwtTokenParams(TEST_USERNAME, TEST_USER_ID);

            Map<String, Object> claims1 = params.toKlabisClaims();
            Map<String, Object> claims2 = params.toKlabisClaims();

            assertThat(claims1).isNotSameAs(claims2);
            claims1.put("extra", "value");
            assertThat(claims2).doesNotContainKey("extra");
        }
    }
}
