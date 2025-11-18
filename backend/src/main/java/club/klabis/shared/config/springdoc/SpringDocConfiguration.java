package club.klabis.shared.config.springdoc;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.*;

@SecurityScheme(name = "klabis_auth", type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(authorizationCode = @OAuthFlow(authorizationUrl = "/oauth/authorize", tokenUrl = "/oauth/token", scopes = {
                @OAuthScope(name = "openid", description = "openid scope"),
                @OAuthScope(name = "klabis", description = "Klabis API")
        })))
@OpenAPIDefinition(security = {@SecurityRequirement(name = "klabis_auth")})
public class SpringDocConfiguration {
}