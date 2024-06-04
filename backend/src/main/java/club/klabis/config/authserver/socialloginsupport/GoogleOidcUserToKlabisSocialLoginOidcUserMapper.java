package club.klabis.config.authserver.socialloginsupport;

import club.klabis.config.authserver.KlabisOidcUser;
import club.klabis.domain.members.Member;
import club.klabis.domain.members.MemberService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
class GoogleOidcUserToKlabisSocialLoginOidcUserMapper implements SocialLoginOidcUserToKlabisOidcUserMapper {
    @Override
    public String getRegistration() {
        return "google";
    }

    @Override
    public KlabisOidcUser map(OidcIdToken idToken, OidcUserInfo userInfo, Member user, List<String> roles) {
        Set<GrantedAuthority> authorities = roles.stream()
                .map(roleName -> new SimpleGrantedAuthority(roleName))
                .collect(Collectors.toSet());

        Map<String, Object> claims = new HashMap<>();
        //claims.putAll(idToken.getClaims());
        claims.put(StandardClaimNames.SUB, user.getRegistration().toRegistrationId());
//        claims.put(StandardClaimNames.GIVEN_NAME, user.getFirstName());
//        claims.put(StandardClaimNames.MIDDLE_NAME, user.getMiddleName());
//        claims.put(StandardClaimNames.FAMILY_NAME, user.getLastName());
//        claims.put(StandardClaimNames.LOCALE, user.getLocale());
//        claims.put(StandardClaimNames.PICTURE, user.getAvatarUrl());

        OidcIdToken customIdToken = new OidcIdToken(
                idToken.getTokenValue(), idToken.getIssuedAt(), idToken.getExpiresAt(), claims
        );

        KlabisOidcUser oidcUser = new KlabisOidcUser(authorities, customIdToken, userInfo);
//        oidcUser.setId(user.getId());
//        oidcUser.setUsername(user.getUsername());
//        oidcUser.setCreatedAt(user.getCreatedAt());
//        oidcUser.setActive(user.isActive());
        return oidcUser;
    }

    @Override
    public Function<String, Optional<Member>> findMemberFunction(MemberService memberService) {
        return memberService::findByGoogleSubject;
    }
}
