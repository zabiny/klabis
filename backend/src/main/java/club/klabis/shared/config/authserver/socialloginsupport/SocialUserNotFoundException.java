package club.klabis.shared.config.authserver.socialloginsupport;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

public class SocialUserNotFoundException extends OAuth2AuthenticationException {
    private final String oauthSubject;
    private final String clientRegistrationId;

    public static SocialUserNotFoundException fromOidcRequest(OidcUserRequest request) {
        return new SocialUserNotFoundException(request.getIdToken().getSubject(),
                request.getClientRegistration().getRegistrationId());
    }

    public SocialUserNotFoundException(String oauthSubject, String clientRegistrationId) {
        super(new OAuth2Error("user_not_found",
                "User with subject %s from client %s not found".formatted(oauthSubject,
                        clientRegistrationId),
                "/auth/linkUser"));
        this.clientRegistrationId = clientRegistrationId;
        this.oauthSubject = oauthSubject;
    }

    public String getClientRegistrationId() {
        return clientRegistrationId;
    }

    public String getOauthSubject() {
        return oauthSubject;
    }
}
