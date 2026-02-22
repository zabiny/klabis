package com.klabis.members.infrastructure.authorizationserver;

import com.klabis.common.security.AuthorizationServerCustomizer;
import com.klabis.members.MemberDto;
import com.klabis.members.Members;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
class KlabisAuthorizationServerCustomizer implements AuthorizationServerCustomizer {
    private final Members members;

    KlabisAuthorizationServerCustomizer(Members members) {
        this.members = members;
    }

    @Override
    public void customizeIdTokenClaims(String userName, JwtClaimsSet.Builder claimsBuilder) {
        // Add profile claims (given_name, family_name) for OIDC profile scope
        members.findByRegistrationNumber(userName)
                .ifPresent(member -> {
                    claimsBuilder.claim("given_name", member.firstName());
                    claimsBuilder.claim("family_name", member.lastName());
                    claimsBuilder.claim("preferred_username", userName);
                });
    }

    @Override
    public void customizeOidcUserInfo(String userName, Set<String> scopes, OidcUserInfo.Builder builder) {
        members.findByRegistrationNumber(userName).ifPresentOrElse(memberDto -> {
            // User has Member profile - add is_member=true and member claims
            builder.claim("is_member", true);
            addProfileClaims(builder, scopes, memberDto);
            addEmailClaims(builder, scopes, memberDto);
        }, () -> {
            // user without member
            builder.claim("is_member", false);
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
                    .claim("updated_at", member.lastModifiedAt());
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
