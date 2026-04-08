package com.klabis.common.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klabis.oauth2.client")
public class OAuth2ClientProperties {

    private String id = "klabis-web";
    private String secret;
    private String uuid;
    private String redirectUris = "http://localhost:3000/auth/callback,https://localhost:8443/mock/auth/callback.html,https://localhost:8443/auth/callback,http://localhost:3000/silent-renew.html,https://localhost:8443/silent-renew.html";
    private String postLogoutRedirectUris = "http://localhost:3000,https://localhost:8443";
    private String scopes;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getPostLogoutRedirectUris() {
        return postLogoutRedirectUris;
    }

    public void setPostLogoutRedirectUris(String postLogoutRedirectUris) {
        this.postLogoutRedirectUris = postLogoutRedirectUris;
    }

    public String getScopes() {
        return scopes;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }
}
