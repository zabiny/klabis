package com.klabis.authorizationserver;

import com.klabis.common.security.AuthorizationServerCustomizer;
import com.klabis.common.security.KlabisOAuth2ClaimNames;
import com.klabis.common.users.UserId;
import com.klabis.members.LegalGuardianDto;
import com.klabis.members.LegalGuardians;
import com.klabis.members.MemberDto;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Component
class KlabisAuthorizationServerCustomizer implements AuthorizationServerCustomizer {
    private final Members members;
    private final LegalGuardians legalGuardians;
    private final KlabisUserDetailsService klabisUserDetailsService;

    KlabisAuthorizationServerCustomizer(Members members,
                                        LegalGuardians legalGuardians,
                                        KlabisUserDetailsService klabisUserDetailsService) {
        this.members = members;
        this.legalGuardians = legalGuardians;
        this.klabisUserDetailsService = klabisUserDetailsService;
    }

    @Override
    public void customizeAccessTokenClaims(String userName, JwtClaimsSet.Builder claimsBuilder, AuthorizationGrantType grantType) {
        claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, userName);

        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)) {
            Optional<UserId> userId = findUserId(userName);
            userId.ifPresent(id -> claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_USER_ID, id.uuid().toString()));

            userId.flatMap(this::findMember).ifPresent(memberDto ->
                    claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, memberDto.memberId().toString()));
        }
    }

    @Override
    public void customizeIdTokenClaims(String userName, JwtClaimsSet.Builder claimsBuilder, AuthorizationGrantType grantType) {
        if (!AuthorizationGrantType.CLIENT_CREDENTIALS.equals(grantType)) {
            claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_USER_NAME, userName);

            Optional<UserId> userId = findUserId(userName);
            userId.ifPresent(id -> claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_USER_ID, id.uuid().toString()));

            Optional<MemberDto> member = userId.flatMap(this::findMember);
            if (member.isPresent()) {
                MemberDto memberDto = member.get();
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_MEMBER_ID, memberDto.memberId().toString());
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_GIVEN_NAME, memberDto.firstName());
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_FAMILY_NAME, memberDto.lastName());
                claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_PREFERRED_USER_NAME, userName);
            } else {
                userId.flatMap(legalGuardians::findById).ifPresent(guardian -> {
                    claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_GIVEN_NAME, guardian.firstName());
                    claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_FAMILY_NAME, guardian.lastName());
                    claimsBuilder.claim(KlabisOAuth2ClaimNames.CLAIM_PREFERRED_USER_NAME, userName);
                });
            }
        }
    }

    @Override
    public void customizeOidcUserInfo(String userName, Set<String> scopes, OidcUserInfo.Builder builder) {
        Optional<UserId> userId = findUserId(userName);
        Optional<MemberDto> member = userId.flatMap(this::findMember);
        if (member.isPresent()) {
            builder.claim(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, true);
            addProfileClaims(builder, scopes, member.get().firstName(), member.get().lastName(), member.get().lastModifiedAt());
            addEmailClaims(builder, scopes, member.get().email());
            return;
        }

        builder.claim(KlabisOAuth2ClaimNames.USER_INFO_IS_MEMBER, false);
        userId.flatMap(legalGuardians::findById).ifPresent((LegalGuardianDto guardian) -> {
            addProfileClaims(builder, scopes, guardian.firstName(), guardian.lastName(), guardian.lastModifiedAt());
            addEmailClaims(builder, scopes, guardian.email());
        });
    }

    // Membership is decided by the user id, never by the login name: a non-member guardian logs in
    // as EXTnnnn, which is not a registration number.
    private Optional<UserId> findUserId(String userName) {
        return klabisUserDetailsService.loadKlabisUserDetails(userName)
                .map(details -> details.getUser().getId());
    }

    private Optional<MemberDto> findMember(UserId userId) {
        return members.findById(MemberId.fromUserId(userId));
    }

    private void addProfileClaims(OidcUserInfo.Builder builder, Set<String> scopes,
                                  String firstName, String lastName, LocalDateTime lastModifiedAt) {
        if (scopes.contains("profile")) {
            builder.givenName(firstName)
                    .familyName(lastName)
                    .updatedAt(lastModifiedAt.toString());
        }
    }

    private void addEmailClaims(OidcUserInfo.Builder builder, Set<String> scopes, String email) {
        if (scopes.contains("email") && email != null) {
            builder.email(email)
                    .emailVerified(false);
        }
    }
}
