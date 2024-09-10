package club.klabis.config.authserver.socialloginsupport;

import club.klabis.domain.appusers.ApplicationUser;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

import java.util.Optional;

public interface SocialLoginOidcUserToKlabisOidcUserMapper {
    /**
     * Returns registration ID for which is mapper supposed to be used
     */
    String getOAuthClientId();

    Optional<ApplicationUser> findApplicationUserForToken(OidcIdToken token);

}
