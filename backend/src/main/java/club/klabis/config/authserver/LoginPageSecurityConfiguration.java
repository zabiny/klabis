package club.klabis.config.authserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
public class LoginPageSecurityConfiguration {

    public static final String CUSTOM_LOGIN_PAGE = "/auth/login";
    public static final String LOGIN_PAGE_ERROR_MESSAGE_SESSION_ATTRIBUTE = "klabis_login_page_error_message";

    public static RequestMatcher LOGIN_REQUESTS_MATCHER = new OrRequestMatcher(
            AntPathRequestMatcher.antMatcher(CUSTOM_LOGIN_PAGE),
            AntPathRequestMatcher.antMatcher("/oauth2/**"),
            AntPathRequestMatcher.antMatcher("/logout"),
            AntPathRequestMatcher.antMatcher("/login/**"));

    @Bean
    @Order(AuthorizationServerConfiguration.AUTH_SERVER_LOGIN_PAGE)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          AuthenticationFailureHandler socialLoginOAuth2FailureHandler) throws Exception {
        return http
                .securityMatcher(LOGIN_REQUESTS_MATCHER)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(CUSTOM_LOGIN_PAGE)
                        .permitAll()
                        .anyRequest()
                        .authenticated())

                //request cache for requests between Login Page and Authorization server (it's needed if there would be some application UI with own spring security chain to login user)
                //.requestCache(LoginPageSecurityConfiguration::applyAuthServerRequestCache)
                .formLogin(form -> form.loginPage(CUSTOM_LOGIN_PAGE))
                .oauth2Login(oauth -> oauth.failureHandler(socialLoginOAuth2FailureHandler))
                .build();
    }

    public static void applyAuthServerRequestCache(RequestCacheConfigurer<HttpSecurity> httpSecurityRequestCacheConfigurer) {
        httpSecurityRequestCacheConfigurer.requestCache(authServerRequestCache("AUTH_SERVER_SPRING_SECURITY_SAVED_REQUEST"));
    }



    private static RequestCache authServerRequestCache(String sessionAttributeName) {
        // As we have Auth server and App server on same instance, we must distinguish request cache for them otherwise App Server will show failed login (even when it passes and works) because App server's "success" redirect target gets overriden by "success" target redirect URI from Authorization server
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        requestCache.setSessionAttrName(sessionAttributeName);
        requestCache.setMatchingRequestParameterName("_spring_security_authorization_server_redirect_uri");
        return requestCache;
    }
}
