package com.klabis.common.security;

import com.klabis.common.mvc.MvcComponent;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.MethodAuthorizationDeniedHandler;

/**
 * Authorization denied handler that returns a masked placeholder value.
 * Use this when the field existence must be visible but its actual content should be hidden.
 */
@MvcComponent
public class MaskDeniedHandler implements MethodAuthorizationDeniedHandler {

    public static final String MASK_VALUE = "***";

    @Override
    public Object handleDeniedInvocation(MethodInvocation methodInvocation, AuthorizationResult authorizationResult) {
        return MASK_VALUE;
    }
}
