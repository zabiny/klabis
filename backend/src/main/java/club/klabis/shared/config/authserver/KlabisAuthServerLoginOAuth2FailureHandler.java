package club.klabis.shared.config.authserver;

import club.klabis.shared.config.authserver.socialloginsupport.SocialUserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
class KlabisAuthServerLoginOAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final Logger LOG = LoggerFactory.getLogger(KlabisAuthServerLoginOAuth2FailureHandler.class);

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {

        String errorMessage;

        if (exception instanceof SocialUserNotFoundException typedException) {
            // TODO: add handling of joining to existing user (after logging in using username and password) = save subject into session, redirect to login with back url to some "merge" page
            LOG.warn("Social OAuth2 login failed: {}", typedException.getError().getDescription());
            errorMessage = "Tento %s účet není propojen se žádným uživatelem".formatted(typedException.getClientRegistrationId());
        } else if (exception instanceof OAuth2AuthenticationException typedException) {
            LOG.warn("Social OAuth2 login failed: {}", typedException.getError().getDescription());
            errorMessage = switch (typedException.getError().getErrorCode()) {
                case "user_disabled":
                    yield "Uživatel je zablokován";
                default:
                    yield "Neočekávaná chyba při přihlášení: %s".formatted(typedException.getError().getErrorCode());
            };
        } else if (exception instanceof AuthenticationException typedException) {
            LOG.warn("Social OAuth2 login failed: {}", typedException.getMessage());
            errorMessage = typedException.getMessage();
        } else {
            errorMessage = "Neočekávaná chyba při přihlášení";
        }

        request.getSession()
                .setAttribute(LoginPageSecurityConfiguration.LOGIN_PAGE_ERROR_MESSAGE_SESSION_ATTRIBUTE, errorMessage);

        response.sendRedirect(LoginPageSecurityConfiguration.CUSTOM_LOGIN_PAGE);
    }
}

