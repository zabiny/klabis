package club.klabis.config.springdoc;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@SecurityScheme(name = "security_auth", type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(authorizationCode = @OAuthFlow(authorizationUrl = "/oauth2/authorize", tokenUrl = "/oauth2/token", scopes = {
                //@OAuthScope(name = "openid", description = "openid scope")
        })))
public class SpringDocConfiguration {
}