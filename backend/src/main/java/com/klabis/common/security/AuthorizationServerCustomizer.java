package com.klabis.common.security;

import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.util.Set;

public interface AuthorizationServerCustomizer {
    AuthorizationServerCustomizer EMPTY_CUSTOMIZER = new  AuthorizationServerCustomizer() {};

    /**
     * Note: Standard claims (sub, iss, aud, exp, iat, auth_time) are handled
     * automatically by Spring Authorization Server. We only add custom claims here.
     */
    default void customizeAccessTokenClaims(String userName, JwtClaimsSet.Builder claimsBuilder, AuthorizationGrantType grantType) {}

    /**
     * Note: Standard claims (sub, iss, aud, exp, iat, auth_time) are handled
     * automatically by Spring Authorization Server. We only add custom claims here.
     */
    default void customizeIdTokenClaims(String userName, JwtClaimsSet.Builder claimsBuilder, AuthorizationGrantType grantType) {
    }

    default void customizeOidcUserInfo(String userName, Set<String> scopes, OidcUserInfo.Builder builder) {
    }
}
