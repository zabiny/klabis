package club.klabis.config.authserver.socialloginsupport;

import club.klabis.domain.appusers.ApplicationUser;
import club.klabis.domain.appusers.ApplicationUsersRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class GoogleOidcUserToKlabisSocialLoginOidcUserMapper implements SocialLoginOidcUserToKlabisOidcUserMapper {

    private final ApplicationUsersRepository userService;

    GoogleOidcUserToKlabisSocialLoginOidcUserMapper(ApplicationUsersRepository userService) {
        this.userService = userService;
    }

    @Override
    public String getOAuthClientId() {
        return "google";
    }

    @Override
    public Optional<ApplicationUser> findApplicationUserForToken(OidcIdToken token) {
        return userService.findByGoogleSubject(token.getSubject());
    }

}
