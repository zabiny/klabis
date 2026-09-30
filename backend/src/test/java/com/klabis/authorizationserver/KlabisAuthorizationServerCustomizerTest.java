package com.klabis.authorizationserver;

import com.klabis.common.security.KlabisOAuth2ClaimNames;
import com.klabis.common.users.domain.User;
import com.klabis.common.users.domain.UserPermissions;
import com.klabis.members.MemberDto;
import com.klabis.members.LegalGuardianDto;
import com.klabis.members.LegalGuardians;
import com.klabis.members.MemberId;
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
import static org.mockito.Mockito.lenient;
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
    private LegalGuardians legalGuardians;

    @Mock
    private KlabisUserDetailsService klabisUserDetailsService;

    private final User testUser = createTestUser();

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
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims()).containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME);
        }

        @Test
        @DisplayName("should add user_id claim when user details exist for authorization_code grant")
        void shouldAddUserIdClaimWhenUserDetailsExist() {
            User mockUser = testUser;
            UUID userId = mockUser.getId().uuid();
            UserPermissions mockPermissions = UserPermissions.empty(mockUser.getId());
            KlabisUserDetailsService.KlabisUserDetails userDetails =
                    new KlabisUserDetailsService.KlabisUserDetails(mockUser, mockPermissions);

            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(userDetails));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims()).containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_ID, userId.toString());
        }

        @Test
        @DisplayName("should add member_id claim when member exists for authorization_code grant")
        void shouldAddMemberIdClaimWhenMemberExists() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims()).containsEntry(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, TEST_MEMBER_ID.toString());
        }

        @Test
        @DisplayName("should add all claims for user with member profile")
        void shouldAddAllClaimsForUserWithMemberProfile() {
            User mockUser = testUser;
            UUID userId = mockUser.getId().uuid();
            UserPermissions mockPermissions = UserPermissions.empty(mockUser.getId());
            KlabisUserDetailsService.KlabisUserDetails userDetails =
                    new KlabisUserDetailsService.KlabisUserDetails(mockUser, mockPermissions);
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);

            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(userDetails));
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
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
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeAccessTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.CLIENT_CREDENTIALS);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, TEST_USERNAME)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_USER_ID)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID);

            verifyNoInteractions(klabisUserDetailsService, members, legalGuardians);
        }

        @Test
        @DisplayName("should handle missing user details gracefully")
        void shouldHandleMissingUserDetailsGracefully() {
            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.empty());
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
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
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.empty());
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
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
            User mockUser = testUser;
            UUID userId = mockUser.getId().uuid();
            UserPermissions mockPermissions = UserPermissions.empty(mockUser.getId());
            KlabisUserDetailsService.KlabisUserDetails userDetails =
                    new KlabisUserDetailsService.KlabisUserDetails(mockUser, mockPermissions);
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);

            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(userDetails));
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
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
            User mockUser = testUser;
            UUID userId = mockUser.getId().uuid();
            UserPermissions mockPermissions = UserPermissions.empty(mockUser.getId());
            KlabisUserDetailsService.KlabisUserDetails userDetails =
                    new KlabisUserDetailsService.KlabisUserDetails(mockUser, mockPermissions);

            when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(userDetails));
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.empty());
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
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
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                    .subject("test-client"); // Add required subject to avoid empty claims

            customizer.customizeIdTokenClaims(TEST_USERNAME, claimsBuilder, AuthorizationGrantType.CLIENT_CREDENTIALS);

            JwtClaimsSet claims = claimsBuilder.build();
            assertThat(claims.getClaims())
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_USER_NAME)
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_USER_ID);

            verifyNoInteractions(klabisUserDetailsService, members, legalGuardians);
        }
    }

    @Nested
    @DisplayName("customizeOidcUserInfo")
    class CustomizeOidcUserInfo {

        @Test
        @DisplayName("should add is_member=true and profile claims with profile scope")
        void shouldAddIsMemberTrueAndProfileClaims() {
            MemberDto memberDto = new MemberDto(TEST_MEMBER_ID, "Jan", "Novák", "jan@example.com", TEST_MODIFIED_AT);
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

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
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

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
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

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
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

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
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.empty());
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

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
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

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
            stubUserDetails();
            when(members.findById(testMemberId())).thenReturn(Optional.of(memberDto));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(TEST_USERNAME, Set.of("profile"), builder);
            OidcUserInfo userInfo = builder.build();

            assertThat(userInfo.getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true)
                    .doesNotContainKey("email")
                    .doesNotContainKey("email_verified");
        }
    }

    @Nested
    @DisplayName("legal guardian without member profile")
    class LegalGuardianWithoutMember {

        private static final String GUARDIAN_LOGIN = "EXT0001";

        private void stubGuardian() {
            User guardianUser = User.createdUser(GUARDIAN_LOGIN, "encodedPassword");
            when(klabisUserDetailsService.loadKlabisUserDetails(GUARDIAN_LOGIN)).thenReturn(Optional.of(
                    new KlabisUserDetailsService.KlabisUserDetails(guardianUser, UserPermissions.empty(guardianUser.getId()))));
            lenient().when(legalGuardians.findById(guardianUser.getId())).thenReturn(Optional.of(
                    new LegalGuardianDto(guardianUser.getId().uuid(), "Petr", "Rodič", "petr@example.com", TEST_MODIFIED_AT)));
        }

        @Test
        @DisplayName("should not resolve EXT login as registration number")
        void shouldNotLookUpMemberByLoginName() {
            stubGuardian();
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

            customizer.customizeAccessTokenClaims(GUARDIAN_LOGIN, JwtClaimsSet.builder(), AuthorizationGrantType.AUTHORIZATION_CODE);

            org.mockito.Mockito.verify(members, org.mockito.Mockito.never()).findByRegistrationNumber(org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @DisplayName("should add names of the guardian to id token and no member_id")
        void shouldAddGuardianNamesToIdToken() {
            stubGuardian();
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();

            customizer.customizeIdTokenClaims(GUARDIAN_LOGIN, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            assertThat(claimsBuilder.build().getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_GIVEN_NAME, "Petr")
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_FAMILY_NAME, "Rodič")
                    .doesNotContainKey(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID);
        }

        @Test
        @DisplayName("should report is_member=false with guardian names in userinfo")
        void shouldReportNotMemberWithGuardianNames() {
            stubGuardian();
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(GUARDIAN_LOGIN, Set.of("profile", "email"), builder);

            assertThat(builder.build().getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, false)
                    .containsEntry("given_name", "Petr")
                    .containsEntry("family_name", "Rodič")
                    .containsEntry("email", "petr@example.com");
        }

        @Test
        @DisplayName("should report is_member=true for guardian promoted to member while keeping EXT login")
        void shouldReportMemberForPromotedGuardian() {
            User promoted = User.createdUser(GUARDIAN_LOGIN, "encodedPassword");
            when(klabisUserDetailsService.loadKlabisUserDetails(GUARDIAN_LOGIN)).thenReturn(Optional.of(
                    new KlabisUserDetailsService.KlabisUserDetails(promoted, UserPermissions.empty(promoted.getId()))));
            when(members.findById(MemberId.fromUserId(promoted.getId())))
                    .thenReturn(Optional.of(new MemberDto(promoted.getId().uuid(), "Petr", "Rodič", "petr@example.com", TEST_MODIFIED_AT)));
            customizer = new KlabisAuthorizationServerCustomizer(members, legalGuardians, klabisUserDetailsService);

            OidcUserInfo.Builder builder = OidcUserInfo.builder();
            customizer.customizeOidcUserInfo(GUARDIAN_LOGIN, Set.of("profile"), builder);
            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder();
            customizer.customizeAccessTokenClaims(GUARDIAN_LOGIN, claimsBuilder, AuthorizationGrantType.AUTHORIZATION_CODE);

            assertThat(builder.build().getClaims()).containsEntry(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true);
            assertThat(claimsBuilder.build().getClaims())
                    .containsEntry(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, promoted.getId().uuid().toString());
        }
    }

    private void stubUserDetails() {
        lenient().when(klabisUserDetailsService.loadKlabisUserDetails(TEST_USERNAME)).thenReturn(Optional.of(
                new KlabisUserDetailsService.KlabisUserDetails(testUser, UserPermissions.empty(testUser.getId()))));
    }

    private MemberId testMemberId() {
        return MemberId.fromUserId(testUser.getId());
    }

    private User createTestUser() {
        return User.createdUser(TEST_USERNAME, "encodedPassword");
    }
}
