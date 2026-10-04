package com.klabis.common.authorization;

import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.ReadAuthority;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

/**
 * Security annotations of a record component accessor (or of the interface method a non-record payload exposes).
 */
record FieldRules(List<Authority> authorities, boolean ownerVisible, List<Authority> readAuthorities) {

    static FieldRules of(Method accessor) {
        HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);
        ReadAuthority readAuthority = accessor.getAnnotation(ReadAuthority.class);
        return new FieldRules(
                hasAuthority != null ? List.of(hasAuthority.value()) : List.of(),
                accessor.getAnnotation(OwnerVisible.class) != null,
                readAuthority != null ? List.of(readAuthority.value()) : List.of());
    }

    /**
     * The method carrying the security annotations of a request property: the record component accessor, or for
     * non-record payloads the interface method of that name. {@code null} when the property is not secured.
     */
    static @Nullable Method securedAccessor(@Nullable Class<?> payloadType, String property) {
        if (payloadType == null) {
            return null;
        }
        if (payloadType.isRecord()) {
            return Arrays.stream(payloadType.getRecordComponents())
                    .filter(component -> component.getName().equals(property))
                    .map(RecordComponent::getAccessor)
                    .filter(accessor -> !of(accessor).isOpen())
                    .findFirst()
                    .orElse(null);
        }
        return Arrays.stream(payloadType.getInterfaces())
                .flatMap(iface -> Arrays.stream(iface.getMethods()))
                .filter(method -> method.getName().equals(property) && method.getParameterCount() == 0)
                .filter(method -> !of(method).isOpen())
                .findFirst()
                .orElse(null);
    }

    boolean isOpen() {
        return authorities.isEmpty() && !ownerVisible;
    }

    String describe() {
        return RequirementDescription.of(authorities, null, ownerVisible);
    }
}
