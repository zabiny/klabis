package com.klabis.common.security;

import com.klabis.common.mvc.MvcComponent;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.MethodAuthorizationDeniedHandler;

/**
 * Authorization denied handler that returns null, causing the field to be absent from JSON.
 * Works with {@code @JsonInclude(NON_NULL)} on the DTO — denied fields simply disappear.
 */
@MvcComponent
public class NullDeniedHandler implements MethodAuthorizationDeniedHandler {

    @Override
    public Object handleDeniedInvocation(MethodInvocation methodInvocation, AuthorizationResult authorizationResult) {
        return null;
    }
}
