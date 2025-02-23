package club.klabis.config.authserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationContext;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationEndpointConfigurer;

import java.util.List;
import java.util.function.Consumer;

/**
 * A customizer for the OAuth2 Authorization Endpoint that enhances the default redirect URI validation.
 * This class allows support for wildcard patterns in redirect URIs for a specific set of client IDs.
 * It extends the default redirect URI validator by adding functionality to match wildcard redirect URIs
 * against a list of allowed patterns for registered clients.
 *
 * The primary use case for this customizer is to enable specific OAuth2 clients to use wildcard redirect URIs,
 * while maintaining security through pattern matching. The customizer decorates the default redirect URI validator
 * by applying additional rules for clients that are explicitly allowed to use wildcard patterns.
 */
class WildcardRedirectUriForOAuth2AuthorizationEndpointCustomizer implements Customizer<OAuth2AuthorizationEndpointConfigurer> {
    private static final Logger LOG = LoggerFactory.getLogger(
            WildcardRedirectUriForOAuth2AuthorizationEndpointCustomizer.class);

    private final List<String> allowPatternsForRedirectUrisForClientIds;

    public WildcardRedirectUriForOAuth2AuthorizationEndpointCustomizer(@NonNull List<String> allowPatternsForRedirectUrisForClientIds) {
        this.allowPatternsForRedirectUrisForClientIds = allowPatternsForRedirectUrisForClientIds;
    }

    // decorates default redirect_uri validator with extension allowing to use wildcard redirect_uri for selected client ids
    @Override
    public void customize(OAuth2AuthorizationEndpointConfigurer oAuth2AuthorizationEndpointConfigurer) {
        oAuth2AuthorizationEndpointConfigurer.authenticationProviders(useWildcardRedirectUrisForGivenRegisteredClients(
                allowPatternsForRedirectUrisForClientIds));
    }

    private Consumer<List<AuthenticationProvider>> useWildcardRedirectUrisForGivenRegisteredClients(List<String> allowedClientIds) {
        return (authenticationProviders) ->
                authenticationProviders.stream()
                        .filter(OAuth2AuthorizationCodeRequestAuthenticationProvider.class::isInstance)
                        .map(OAuth2AuthorizationCodeRequestAuthenticationProvider.class::cast)
                        .forEach((authenticationProvider) -> {
                            Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> authenticationValidator =
                                    // Override default redirect_uri validator
                                    new AllowWildcardRedirectUriPatternForSelectedClients(allowedClientIds)
                                            // Reuse default scope validator
                                            .andThen(OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR);

                            authenticationProvider.setAuthenticationValidator(authenticationValidator);
                        });
    }

    static class AllowWildcardRedirectUriPatternForSelectedClients implements Consumer<OAuth2AuthorizationCodeRequestAuthenticationContext> {

        private final List<String> allowedClientIds;


        AllowWildcardRedirectUriPatternForSelectedClients(List<String> allowedClientIds) {
            this.allowedClientIds = allowedClientIds;
        }

        private boolean canUseWildcardRedirectUrisForClient(RegisteredClient registeredClient) {
            return allowedClientIds.contains(registeredClient.getClientId());
        }

        private boolean matchesAnyRegisteredRedirectUriPattern(RegisteredClient registeredClient, String requestedRedirectUri) {
            return registeredClient.getRedirectUris().stream()
                    .anyMatch(redirectUri -> requestedRedirectUri.matches(redirectUri.replace("*", ".*")));
        }

        @Override
        public void accept(OAuth2AuthorizationCodeRequestAuthenticationContext authenticationContext) {
            OAuth2AuthorizationCodeRequestAuthenticationToken authorizationCodeRequestAuthentication =
                    authenticationContext.getAuthentication();
            RegisteredClient registeredClient = authenticationContext.getRegisteredClient();
            String requestedRedirectUri = authorizationCodeRequestAuthentication.getRedirectUri();

            // if registeredClient id is in allowed clientIds and requestedRedirectUri matches any pattern from client redirectUris, then add requestedRedirectUri into registered client's redirect uris
            if (canUseWildcardRedirectUrisForClient(registeredClient) && matchesAnyRegisteredRedirectUriPattern(
                    registeredClient,
                    requestedRedirectUri)) {
                LOG.trace(
                        "Redirect URI '{}' is not allowed for client '{}' but client has allowed patterns for redirect_uris and this uri matches one of them - adding this uri to client's allowed uris",
                        requestedRedirectUri,
                        registeredClient.getClientId());
                registeredClient.getRedirectUris().add(requestedRedirectUri);
            }

            // delegate to default redirect URI validator
            OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR.accept(
                    authenticationContext);
        }
    }
}
