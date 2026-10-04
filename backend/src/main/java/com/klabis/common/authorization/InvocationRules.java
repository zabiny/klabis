package com.klabis.common.authorization;

import com.klabis.common.security.MethodSecurityAnnotations;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Security annotations of a controller or service method: the {@link HasAuthority} of the method (or else of its
 * class), {@link OwnerVisible} and the parameter marked {@link TargetId}, each also found on the interface the
 * method implements.
 */
record InvocationRules(List<Authority> authorities, boolean ownerVisible, TargetParameter target) {

    static InvocationRules of(Method method, Class<?> targetClass) {
        HasAuthority hasAuthority = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, HasAuthority.class);
        if (hasAuthority == null) {
            hasAuthority = MethodSecurityAnnotations.findClassAnnotation(targetClass, HasAuthority.class);
        }
        List<Authority> authorities = hasAuthority != null ? List.of(hasAuthority.value()) : List.of();
        boolean ownerVisible = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, OwnerVisible.class) != null;

        int index = MethodSecurityAnnotations.findAnnotatedParameterIndex(method, targetClass, TargetId.class);
        TargetId targetId = MethodSecurityAnnotations.findParameterAnnotation(method, targetClass, index, TargetId.class);
        TargetParameter target = targetId != null ? new TargetParameter(index, targetId.value()) : null;
        return new InvocationRules(authorities, ownerVisible, target);
    }

    boolean isOpen() {
        return authorities.isEmpty() && !ownerVisible;
    }

    String describe() {
        return RequirementDescription.of(authorities, target != null ? target.type() : null, ownerVisible);
    }
}
