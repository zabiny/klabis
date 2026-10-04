package com.klabis.common.authorization;

import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.ReadAuthority;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;

import java.lang.reflect.Method;
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

    boolean isOpen() {
        return authorities.isEmpty() && !ownerVisible;
    }
}
