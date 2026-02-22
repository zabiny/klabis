package com.klabis.common.security;

import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.util.Set;

public interface AuthorizationServerCustomizer {
    AuthorizationServerCustomizer EMPTY_CUSTOMIZER = new  AuthorizationServerCustomizer() {};


    default void customizeIdTokenClaims(String userName, JwtClaimsSet.Builder claimsBuilder) {
    }

    default void customizeOidcUserInfo(String userName, Set<String> scopes, OidcUserInfo.Builder builder) {
    }
}
