package club.klabis.shared.config.authserver.socialloginsupport;

import club.klabis.members.MemberId;
import club.klabis.members.application.MembersRepository;
import club.klabis.members.domain.Member;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
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
    private final MembersRepository membersRepository;

    public CustomOidcUserService(List<SocialLoginOidcUserToKlabisOidcUserMapper> mappers, MembersRepository membersRepository) {
        this.mappers = mappers.stream()
                .collect(Collectors.toMap(SocialLoginOidcUserToKlabisOidcUserMapper::getOAuthClientId,
                        Function.identity()));
        this.membersRepository = membersRepository;
    }

    private Optional<SocialLoginOidcUserToKlabisOidcUserMapper> getMapperForRegistrationId(ClientRegistration registration) {
        return mappers.values()
                .stream()
                .filter(it -> registration.getRegistrationId().equals(it.getOAuthClientId()))
                .findAny();
    }

    @Override
    public DefaultOidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        SocialLoginOidcUserToKlabisOidcUserMapper mapper = getMapperForRegistrationId(userRequest.getClientRegistration())
                .orElseThrow(() -> new IllegalStateException("No OIDC mapper defined for registrationId %s".formatted(
                        userRequest.getClientRegistration().getRegistrationId())));

        ApplicationUser appUser = mapper.findApplicationUserForToken(userRequest.getIdToken())
                .orElseThrow(() -> SocialUserNotFoundException.fromOidcRequest(userRequest));

        if (appUser.isDisabled()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("user_disabled",
                    "User %s is disabled".formatted(appUser.getUsername()),
                    "auth/userIsDisabled"));
        }

        return createAuthentication(oidcUser.getIdToken(),
                oidcUser.getUserInfo(),
                appUser,
                List.of());
    }

    DefaultOidcUser createAuthentication(OidcIdToken idToken, OidcUserInfo userInfo, ApplicationUser user, List<String> roles) {
        Set<GrantedAuthority> authorities = roles.stream()
                .map(roleName -> new SimpleGrantedAuthority(roleName))
                .collect(Collectors.toSet());

        MemberId memberId = membersRepository.findMemberByAppUserId(user.getId()).map(Member::getId).orElse(null);

        Map<String, Object> klabisClaims = new HashMap<>();
        //claims.putAll(idToken.getClaims());
        klabisClaims.put(StandardClaimNames.SUB, user.getUsername());
        klabisClaims.put("memberId", memberId);
        klabisClaims.put("appUserId", user.getId());
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