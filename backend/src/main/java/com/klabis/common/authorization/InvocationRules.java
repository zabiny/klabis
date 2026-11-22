package com.klabis.common.authorization;

import com.klabis.common.security.MethodSecurityAnnotations;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Security annotations of a controller or service method: the {@link HasAuthority} of the method (or else of its
 * class), {@link OwnerVisible} and the parameters marked {@link TargetId}, each also found on the interface the
 * method implements. A method identifies at most one target per {@link TargetType}, so each authority is checked
 * over the target of its own kind.
 */
record InvocationRules(List<Authority> authorities, boolean ownerVisible, Map<TargetType, TargetParameter> targets) {

    static InvocationRules of(Method method, Class<?> targetClass) {
        HasAuthority hasAuthority = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, HasAuthority.class);
        if (hasAuthority == null) {
            hasAuthority = MethodSecurityAnnotations.findClassAnnotation(targetClass, HasAuthority.class);
        }
        List<Authority> authorities = hasAuthority != null ? List.of(hasAuthority.value()) : List.of();
        boolean ownerVisible = MethodSecurityAnnotations.findMethodAnnotation(method, targetClass, OwnerVisible.class) != null;

        Map<TargetType, TargetParameter> targets = new EnumMap<>(TargetType.class);
        for (int index = 0; index < method.getParameterCount(); index++) {
            TargetId targetId = MethodSecurityAnnotations.findParameterAnnotation(method, targetClass, index, TargetId.class);
            if (targetId == null) {
                continue;
            }
            if (targets.putIfAbsent(targetId.value(), new TargetParameter(index, targetId.value())) != null) {
                throw new IllegalStateException("%s identifies more than one %s target with @TargetId"
                        .formatted(method, targetId.value()));
            }
        }
        return new InvocationRules(authorities, ownerVisible, Map.copyOf(targets));
    }

    boolean isOpen() {
        return authorities.isEmpty() && !ownerVisible;
    }

    @Nullable TargetParameter targetOfKind(TargetType kind) {
        return targets.get(kind);
    }

    String describe() {
        return RequirementDescription.of(authorities, targets.keySet(), ownerVisible);
    }
}
