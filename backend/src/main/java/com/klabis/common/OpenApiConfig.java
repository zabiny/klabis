package com.klabis.common;

import com.klabis.users.Authority;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) configuration.
 * <p>
 * Configures API documentation with:
 * - General API information
 * - OAuth2 security scheme
 * - Server URLs
 * - Contact and license information
 * <p>
 * Access Swagger UI at: /swagger-ui/index.html
 * Access OpenAPI spec at: /v3/api-docs
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Klabis Orienteering Club Management API",
                version = "0.1.0",
                description = """
                        RESTful API for managing orienteering club operations.
                        
                        ## Features
                        - Member registration and management
                        - HATEOAS-compliant with HAL+FORMS media type
                        - OAuth2 authentication and authorization
                        - RFC 7807 Problem Details for errors
                        
                        ## Authentication
                        Most endpoints require OAuth2 authentication. Use the 'Authorize' button to authenticate.
                        """,
                contact = @Contact(
                        name = "Klabis Development Team",
                        email = "davca7@gmail.com"
                ),
                license = @License(
                        name = "Proprietary",
                        url = "https://github.com/zabiny/klabis/blob/main/LICENCE"
                )
        ),
        servers = {
                @Server(
                        url = "https://localhost:8443",
                        description = "Local development server"
                ),
                @Server(
                        url = "https://api.klabis.com",
                        description = "Production server"
                )
        }
)
@SecurityScheme(
        name = "KlabisAuth",
        type = SecuritySchemeType.OAUTH2,
        description = "OAuth2 authentication with Klabis Authorization Server",
        flows = @OAuthFlows(authorizationCode = @OAuthFlow(authorizationUrl = "/oauth2/authorize", tokenUrl = "/oauth2/token", scopes = {
                @OAuthScope(name = "openid", description = "authenticate"),
                @OAuthScope(name = "profile", description = "display user profile information"),
                @OAuthScope(name = Authority.MEMBERS_SCOPE, description = "Members"),
                @OAuthScope(name = Authority.EVENTS_SCOPE, description = "Events"),
        }))
)
public class OpenApiConfig {
}
