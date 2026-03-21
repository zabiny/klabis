package com.klabis.common.security;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.MethodAuthorizationDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Authorization denied handler that returns a masked placeholder value.
 * Use this when the field existence must be visible but its actual content should be hidden.
 */
@Component
public class MaskDeniedHandler implements MethodAuthorizationDeniedHandler {

    public static final String MASK_VALUE = "***";

    @Override
    public Object handleDeniedInvocation(MethodInvocation methodInvocation, AuthorizationResult authorizationResult) {
        return MASK_VALUE;
    }
}
