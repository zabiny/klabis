package club.klabis.config.authserver.ui;

import club.klabis.config.authserver.LoginPageSecurityConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
class AuthServerLoginController {

    private final RegisteredOAuthProviders providers;

    AuthServerLoginController(ClientRegistrationRepository registrationRepository) {
        this.providers = new RegisteredOAuthProviders(registrationRepository);
    }

    @GetMapping(value = {LoginPageSecurityConfiguration.CUSTOM_LOGIN_PAGE})
    public ModelAndView login() {
        ModelAndView view = new ModelAndView("ui/klabisOAuth");
        view.addObject("oauthProviders", providers);
        view.addObject("submitUrl", LoginPageSecurityConfiguration.CUSTOM_LOGIN_PAGE);
        return view;
    }

}

class RegisteredOAuthProviders {
    private final ClientRegistrationRepository clientRegistrationRepository;
    private static final Logger LOG = LoggerFactory.getLogger(RegisteredOAuthProviders.class);

    RegisteredOAuthProviders(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    public boolean isGoogle() {
        return clientRegistrationRepository.findByRegistrationId("google") != null;
    }

    public boolean isGithub() {
        return clientRegistrationRepository.findByRegistrationId("github") != null;
    }

    public boolean isSocialLoginEnabled() {
        return isGoogle() || isGithub();
    }
}