package com.klabis.common.security.fieldsecurity;

import com.klabis.common.mvc.MvcComponent;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.authorization.method.MethodAuthorizationDeniedHandler;

@MvcComponent
public class MaskDeniedHandler implements MethodAuthorizationDeniedHandler {

    public static final String MASK_VALUE = "***";

    @Override
    public Object handleDeniedInvocation(MethodInvocation methodInvocation, AuthorizationResult authorizationResult) {
        return MASK_VALUE;
    }
}
