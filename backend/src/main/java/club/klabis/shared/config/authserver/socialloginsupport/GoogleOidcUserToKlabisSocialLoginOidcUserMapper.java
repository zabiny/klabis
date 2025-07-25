package club.klabis.shared.config.authserver.socialloginsupport;

import club.klabis.users.application.ApplicationUsersRepository;
import club.klabis.users.domain.ApplicationUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
class GoogleOidcUserToKlabisSocialLoginOidcUserMapper implements SocialLoginOidcUserToKlabisOidcUserMapper {

    private final ApplicationUsersRepository usersRepository;

    GoogleOidcUserToKlabisSocialLoginOidcUserMapper(ApplicationUsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    @Override
    public String getOAuthClientId() {
        return "google";
    }

    @Override
    public Optional<ApplicationUser> findApplicationUserForToken(OidcIdToken token) {
        return usersRepository.findByGoogleSubject(token.getSubject());
    }

}
