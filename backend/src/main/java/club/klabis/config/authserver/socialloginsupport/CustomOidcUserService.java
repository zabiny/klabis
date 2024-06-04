package club.klabis.config.authserver.socialloginsupport;

import club.klabis.config.authserver.KlabisOidcUser;
import club.klabis.domain.users.MemberService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class CustomOidcUserService extends OidcUserService {
    private final MemberService userService;
    private final Map<String, SocialLoginOidcUserToKlabisOidcUserMapper> mappers;

    public CustomOidcUserService(List<SocialLoginOidcUserToKlabisOidcUserMapper> mappers, MemberService userService) {
        this.mappers = mappers.stream().collect(Collectors.toMap(SocialLoginOidcUserToKlabisOidcUserMapper::getRegistration, Function.identity()));
        this.userService = userService;
    }

    private Optional<SocialLoginOidcUserToKlabisOidcUserMapper> getMapperForRegistrationId(ClientRegistration registration) {
        return mappers.values().stream().filter(it -> registration.getRegistrationId().equals(it.getRegistration())).findAny();
    }

    @Override
    public KlabisOidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        SocialLoginOidcUserToKlabisOidcUserMapper mapper = getMapperForRegistrationId(userRequest.getClientRegistration())
                .orElseThrow(() -> new RuntimeException("No OIDC mapper defined for registrationId %s".formatted(userRequest.getClientRegistration().getRegistrationId())));

        String tokenSubject = userRequest.getIdToken().getSubject();
        return mapper.findMemberFunction(userService)
                .apply(tokenSubject)
                .map(member -> mapper.map(oidcUser.getIdToken(), oidcUser.getUserInfo(), member, List.of()))
                .orElseThrow(() -> new OAuth2AuthenticationException("User with google subject %s not found!".formatted(tokenSubject)));
    }
}