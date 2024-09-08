package club.klabis.config.authserver.socialloginsupport;

import club.klabis.config.authserver.KlabisOidcUser;
import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import club.klabis.domain.members.Member;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface SocialLoginOidcUserToKlabisOidcUserMapper {
    /**
     * Returns registration ID for which is mapper supposed to be used
     */
    String getRegistration();

    Function<String, Optional<ApplicationUser>> findMemberFunction(ApplicationUsersRepository memberService);

    default KlabisOidcUser map(OidcIdToken idToken, OidcUserInfo userInfo, ApplicationUser user, List<String> roles) {
        Set<GrantedAuthority> authorities = roles.stream()
                .map(roleName -> new SimpleGrantedAuthority(roleName))
                .collect(Collectors.toSet());

        Map<String, Object> klabisClaims = new HashMap<>();
        //claims.putAll(idToken.getClaims());
        klabisClaims.put(StandardClaimNames.SUB, user.getUsername());
        klabisClaims.put("memberId", user.getMemberId().orElse(null));
//        claims.put(StandardClaimNames.GIVEN_NAME, user.getFirstName());
//        claims.put(StandardClaimNames.MIDDLE_NAME, user.getMiddleName());
//        claims.put(StandardClaimNames.FAMILY_NAME, user.getLastName());
//        claims.put(StandardClaimNames.LOCALE, user.getLocale());
//        claims.put(StandardClaimNames.PICTURE, user.getAvatarUrl());

        OidcIdToken customIdToken = new OidcIdToken(
                idToken.getTokenValue(), idToken.getIssuedAt(), idToken.getExpiresAt(), klabisClaims
        );

        KlabisOidcUser oidcUser = new KlabisOidcUser(authorities, customIdToken, userInfo);
//        oidcUser.setId(user.getId());
//        oidcUser.setUsername(user.getUsername());
//        oidcUser.setCreatedAt(user.getCreatedAt());
//        oidcUser.setActive(user.isActive());
        return oidcUser;
    };

}
