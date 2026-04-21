package com.klabis.authorizationserver;

import com.klabis.common.security.KlabisOAuth2ClaimNames;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserPermissions;
import com.klabis.members.MemberDto;
import com.klabis.members.Members;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("KlabisAuthorizationServerCustomizer Tests")
class KlabisAuthorizationServerCustomizerTest {

    private static final String TEST_USERNAME = "ZBM8001";
    private static final UUID TEST_MEMBER_ID = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
    private static final LocalDateTime TEST_MODIFIED_AT = LocalDateTime.of(2024, 1, 15, 10, 30);

    @Mock
    private Members members;

    @Mock
    private KlabisUserDetailsService klabisUserDetailsService;

    private KlabisAuthorizationServerCustomizer customizer;

    @AfterEach
    void tearDown() {
        customizer = null;
    }

    @Nested
    @DisplayName("customizeAccessTokenClaims")
    class CustomizeAccessTokenClaims {

        @Test
        @DisplayName("should add user_name claim for authorization_code grant")
        void shouldAddUserNameClaimForAuthorizationCodeGrant() {
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims()).containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME);
        }

        @Test
        @DisplayName("should add user_id claim when user details exist for authorization_code grant")
        void shouldAddUserIdClaimWhenUserDetailsExist() {
            User mockUser = createTestUser();
            UUID userId = mockUser.getId().uuid();
            UserPermissions mockPermissions = UserPermissions.empty(mockUser.getId());
            KlabisUserDetailsService.KlabisUserDetails userDetails =
                    new KlabisUserDetailsService.KlabisUserDetails(mockUser, mockPermissions);

            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(userDetails));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims()).containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_ID, userId.toString());
        }

        @Test
        @DisplayName("should add member_id claim when member exists for authorization_code grant")
        void shouldAddMemberIdClaimWhenMemberExists() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims()).containsEntry(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, TEST_MEMBER_ID.toString());
        }

        @Test
        @DisplayName("should add all claims for user with member profile")
        void shouldAddAllClaimsForUserWithMemberProfile() {
            User mockUser = createTestUser();
            UUID userId = mockUser.getId().uuid();
            UserPermissions mockPermissions = UserPermissions.empty(mockUser.getId());
            KlabisUserDetailsService.KlabisUserDetails userDetails =
                    new KlabisUserDetailsService.KlabisUserDetails(mockUser, mockPermissions);
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);

            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(userDetails));
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME)
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_ID, userId.toString())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, TEST_MEMBER_ID.toString());
        }

        @Test
        @DisplayName("should add only user_name for client_credentials grant")
        void shouldAddOnlyUserNameForClientCredentialsGrant() {
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.CLIENT_CREDENTIALS);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_USER_ID)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID);

            verifyNoInteractions(klabisUserDetailsService, members);
        }

        @Test
        @DisplayName("should handle missing user details gracefully")
        void shouldHandleMissingUserDetailsGracefully() {
            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.empty());
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_USER_ID);
        }

        @Test
        @DisplayName("should handle missing member gracefully")
        void shouldHandleMissingMemberGracefully() {
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.empty());
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID);
        }
    }

    @Nested
    @DisplayName("customizeIdTokenClaims")
    class CustomizeIdTokenClaims {

        @Test
        @DisplayName("should add profile claims when member exists")
        void shouldAddProfileClaimsWhenMemberExists() {
            User mockUser = createTestUser();
            UUID userId = mockUser.getId().uuid();
            UserPermissions mockPermissions = UserPermissions.empty(mockUser.getId());
            KlabisUserDetailsService.KlabisUserDetails userDetails =
                    new KlabisUserDetailsService.KlabisUserDetails(mockUser, mockPermissions);
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);

            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(userDetails));
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeIdTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME)
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_ID, userId.toString())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, TEST_MEMBER_ID.toString())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_GIVEN_NAME, "Jan")
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_FAMILY_NAME, "Novák")
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_PREFERRED_USER_NAME, TEST_USERNAME);
        }

        @Test
        @DisplayName("should add only user_name and user_id for user without member")
        void shouldAddOnlyUserNameAndUserIdForUserWithoutMember() {
            User mockUser = createTestUser();
            UUID userId = mockUser.getId().uuid();
            UserPermissions mockPermissions = UserPermissions.empty(mockUser.getId());
            KlabisUserDetailsService.KlabisUserDetails userDetails =
                    new KlabisUserDetailsService.KlabisUserDetails(mockUser, mockPermissions);

            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(userDetails));
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.empty());
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeIdTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME)
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_ID, userId.toString())
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_GIVEN_NAME)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_FAMILY_NAME);
        }

        @Test
        @DisplayName("should add no claims for client_credentials grant")
        void shouldAddNoClaimsForClientCredentialsGrant() {
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                    .subject("test-client"); // Add required subject to avoid empty claims

            customizer.customizeIdTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.CLIENT_CREDENTIALS);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_USER_NAME)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_USER_ID);

            verifyNoInteractions(klabisUserDetailsService, members);
        }
    }

    @Nested
    @DisplayName("customizeOidcUserInfo")
    class CustomizeOidcUserInfo {

        @Test
        @DisplayName("should add is_member=true and profile claims with profile scope")
        void shouldAddIsMemberTrueAndProfileClaims() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(TEST_USERNAME, Set.of("profile"), builder);
            OidcUserInfo userInfo = builder.build();

            assertThat(userInfo.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true)
                    .containsEntry("given_name", "Jan")
                    .containsEntry("family_name", "Novák")
                    .containsEntry("updated_at", TEST_MODIFIED_AT.toString());
        }

        @Test
        @DisplayName("should add email claims with email scope when member has email")
        void shouldAddEmailClaimsWhenMemberHasEmail() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(TEST_USERNAME, Set.of("email"), builder);
            OidcUserInfo userInfo = builder.build();

            assertThat(userInfo.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true)
                    .containsEntry("email", "jan@example.com")
                    .containsEntry("email_verified", false);
        }

        @Test
        @DisplayName("should not add email claims when member has no email")
        void shouldNotAddEmailClaimsWhenMemberHasNoEmail() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", null, TEST_MODIFIED_AT);
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(TEST_USERNAME, Set.of("email"), builder);
            OidcUserInfo userInfo = builder.build();

            assertThat(userInfo.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true)
                    .doesNotContainKey("email")
                    .doesNotContainKey("email_verified");
        }

        @Test
        @DisplayName("should add all claims with both profile and email scopes")
        void shouldAddAllClaimsWithBothScopes() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(TEST_USERNAME, Set.of("profile", "email"), builder);
            OidcUserInfo userInfo = builder.build();

            assertThat(userInfo.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true)
                    .containsEntry("given_name", "Jan")
                    .containsEntry("family_name", "Novák")
                    .containsEntry("updated_at", TEST_MODIFIED_AT.toString())
                    .containsEntry("email", "jan@example.com")
                    .containsEntry("email_verified", false);
        }

        @Test
        @DisplayName("should add only is_member=false for user without member")
        void shouldAddOnlyIsMemberFalseForUserWithoutMember() {
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.empty());
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(TEST_USERNAME, Set.of("profile", "email"), builder);
            OidcUserInfo userInfo = builder.build();

            assertThat(userInfo.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, false)
                    .doesNotContainKey("given_name")
                    .doesNotContainKey("family_name")
                    .doesNotContainKey("email");
        }

        @Test
        @DisplayName("should not add profile claims without profile scope")
        void shouldNotAddProfileClaimsWithoutProfileScope() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(TEST_USERNAME, Set.of(), builder);
            OidcUserInfo userInfo = builder.build();

            assertThat(userInfo.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true)
                    .doesNotContainKey("given_name")
                    .doesNotContainKey("family_name")
                    .doesNotContainKey("updated_at");
        }

        @Test
        @DisplayName("should not add email claims without email scope")
        void shouldNotAddEmailClaimsWithoutEmailScope() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);
            when(members.findByRegistrationNumber(TEST_USERNAME)).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(TEST_USERNAME, Set.of("profile"), builder);
            OidcUserInfo userInfo = builder.build();

            assertThat(userInfo.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true)
                    .doesNotContainKey("email")
                    .doesNotContainKey("email_verified");
        }
    }

    private User createTestUser() {
        return User.createdUser(TEST_USERNAME, "encodedPassword");
    }
}
