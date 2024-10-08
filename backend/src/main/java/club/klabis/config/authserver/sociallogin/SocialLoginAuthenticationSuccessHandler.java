package club.klabis.config.authserver.sociallogin;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * <a href="https://docs.spring.io/spring-authorization-server/reference/guides/how-to-social-login.html#advanced-use-cases-capture-users">Social login in authorization server</a>
 */
// TODO: complete link of social login to Klabis App user (currently it's not used)
public class SocialLoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthenticationSuccessHandler delegate = new SavedRequestAwareAuthenticationSuccessHandler();

    private Consumer<OAuth2User> oauth2UserHandler = (user) -> {};

    private Consumer<OidcUser> oidcUserHandler = (user) -> this.oauth2UserHandler.accept(user);

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication
    ) throws IOException, ServletException {
        if (authentication instanceof OAuth2AuthenticationToken) {
            if (authentication.getPrincipal() instanceof OidcUser) {
                this.oidcUserHandler.accept((OidcUser) authentication.getPrincipal());
            } else if (authentication.getPrincipal() instanceof OAuth2User) {
                this.oauth2UserHandler.accept((OAuth2User) authentication.getPrincipal());
            }
        } else if (authentication instanceof UsernamePasswordAuthenticationToken userpasstoken) {
            System.out.println("Handling %s user (username+pass)".formatted(userpasstoken.getPrincipal()));
        }

        this.delegate.onAuthenticationSuccess(request, response, authentication);
    }

    public void setOAuth2UserHandler(Consumer<OAuth2User> oauth2UserHandler) {
        this.oauth2UserHandler = oauth2UserHandler;
    }

    public void setOidcUserHandler(Consumer<OidcUser> oidcUserHandler) {
        this.oidcUserHandler = oidcUserHandler;
    }
}