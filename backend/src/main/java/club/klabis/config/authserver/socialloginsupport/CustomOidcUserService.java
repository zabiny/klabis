package club.klabis.config.authserver.socialloginsupport;

import club.klabis.domain.appusers.ApplicationUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class CustomOidcUserService extends OidcUserService {
    private final Map<String, SocialLoginOidcUserToKlabisOidcUserMapper> mappers;

    public CustomOidcUserService(List<SocialLoginOidcUserToKlabisOidcUserMapper> mappers) {
        this.mappers = mappers.stream().collect(Collectors.toMap(SocialLoginOidcUserToKlabisOidcUserMapper::getOAuthClientId, Function.identity()));
    }

    private Optional<SocialLoginOidcUserToKlabisOidcUserMapper> getMapperForRegistrationId(ClientRegistration registration) {
        return mappers.values().stream().filter(it -> registration.getRegistrationId().equals(it.getOAuthClientId())).findAny();
    }

    @Override
    public DefaultOidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        SocialLoginOidcUserToKlabisOidcUserMapper mapper = getMapperForRegistrationId(userRequest.getClientRegistration())
                .orElseThrow(() -> new RuntimeException("No OIDC mapper defined for registrationId %s".formatted(userRequest.getClientRegistration().getRegistrationId())));

        return mapper.findApplicationUserForToken(userRequest.getIdToken())
                .map(applicationUser -> createAuthentication(oidcUser.getIdToken(), oidcUser.getUserInfo(), applicationUser, List.of()))
                .orElseThrow(() -> new OAuth2AuthenticationException("User with subject %s (%s) not found!".formatted(oidcUser.getSubject(), mapper.getOAuthClientId())));
    }

    DefaultOidcUser createAuthentication(OidcIdToken idToken, OidcUserInfo userInfo, ApplicationUser user, List<String> roles) {
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

        DefaultOidcUser oidcUser = new DefaultOidcUser(authorities, customIdToken, userInfo);
//        oidcUser.setId(user.getId());
//        oidcUser.setUsername(user.getUsername());
//        oidcUser.setCreatedAt(user.getCreatedAt());
//        oidcUser.setActive(user.isActive());
        return oidcUser;
    }

    ;

}