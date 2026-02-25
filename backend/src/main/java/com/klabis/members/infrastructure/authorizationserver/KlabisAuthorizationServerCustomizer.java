package com.klabis.members.infrastructure.authorizationserver;

import com.klabis.common.security.AuthorizationServerCustomizer;
import com.klabis.common.security.KlabisOAuth2ClaimNames;
import com.klabis.common.users.authorization.KlabisUserDetailsService;
import com.klabis.members.MemberDto;
import com.klabis.members.Members;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
class KlabisAuthorizationServerCustomizer implements AuthorizationServerCustomizer {
    private final Members members;
    private final KlabisUserDetailsService klabisUserDetailsService;

    KlabisAuthorizationServerCustomizer(Members members, KlabisUserDetailsService klabisUserDetailsService) {
        this.members = members;
        this.klabisUserDetailsService = klabisUserDetailsService;
    }

    @Override
    public void customizeAccessTokenClaims(String userName, JwtClaimsSet.Builder claimsBuilder, AuthorizationGrantType grantType) {
        claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, userName);

        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)) {
            // Add user_id claim for type-safe access to UserId in controllers/services
            klabisUserDetailsService.loadKlabisUserDetails(userName)
                    .ifPresent(klabisUserDetails -> {
                        claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_USER_ID, klabisUserDetails.getUser().getId().uuid().toString());
                    });

            members.findByRegistrationNumber(userName).ifPresent(memberDto -> {
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, memberDto.memberId().toString());
            });
        }
    }

    @Override
    public void customizeIdTokenClaims(String userName, JwtClaimsSet.Builder claimsBuilder, AuthorizationGrantType grantType) {
        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)) {
            claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, userName);

            // Add user_id claim for type-safe access to UserId in controllers/services
            klabisUserDetailsService.loadKlabisUserDetails(userName)
                    .ifPresent(klabisUserDetails -> {
                        claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_USER_ID, klabisUserDetails.getUser().getId().uuid().toString());
                    });

            members.findByRegistrationNumber(userName).ifPresent(memberDto -> {
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, memberDto.memberId().toString());
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_GIVEN_NAME, memberDto.firstName());
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_FAMILY_NAME, memberDto.lastName());
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_PREFERRED_USER_NAME, userName);
            });
        }
    }

    @Override
    public void customizeOidcUserInfo(String userName, Set<String> scopes, OidcUserInfo.Builder builder) {
        members.findByRegistrationNumber(userName).ifPresentOrElse(memberDto -> {
            // User has Member profile - add is_member=true and member claims
            builder.claim(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true);
            addProfileClaims(builder, scopes, memberDto);
            addEmailClaims(builder, scopes, memberDto);
        }, () -> {
            // user without member
            builder.claim(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, false);
        });

    }


    /**
     * Adds OIDC profile scope claims to UserInfo response for members.
     * <p>
     * Maps Member data to OIDC claims:
     * - given_name (first name)
     * - family_name (last name)
     * - updated_at (profile last modification timestamp)
     * <p>
     * Note: user_name and is_member claims are added by oidcUserInfoMapper before calling this method.
     */
    private void addProfileClaims(OidcUserInfo.Builder builder, Set<String> scopes, MemberDto member) {
        if (scopes.contains("profile")) {
            builder.givenName(member.firstName())
                    .familyName(member.lastName())
                    .updatedAt(member.lastModifiedAt().toString());
        }
    }

    /**
     * Adds OIDC email scope claims to UserInfo response.
     * <p>
     * Task 2.5-2.6: Maps Member email to standard OIDC claims:
     * - email (member's email address)
     * - email_verified (always false until email verification is implemented)
     * <p>
     * Omits email claims if Member has no email (null-safe).
     */
    private void addEmailClaims(OidcUserInfo.Builder builder, Set<String> scopes, MemberDto member) {
        if (scopes.contains("email") && member.email() != null) {
            builder.email(member.email())
                    .emailVerified(false);
        }
    }
}
