package com.klabis.common.authorization;

import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.security.fieldsecurity.ReadAuthority;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Security annotations of a record component accessor (or of the interface method a non-record payload exposes).
 */
record FieldRules(Method accessor, @Nullable PreAuthorize preAuthorize, List<Authority> authorities,
                  boolean ownerVisible, List<Authority> readAuthorities) {

    static FieldRules of(Method accessor) {
        HasAuthority hasAuthority = accessor.getAnnotation(HasAuthority.class);
        ReadAuthority readAuthority = accessor.getAnnotation(ReadAuthority.class);
        return new FieldRules(
                accessor,
                accessor.getAnnotation(PreAuthorize.class),
                hasAuthority != null ? List.of(hasAuthority.value()) : List.of(),
                accessor.getAnnotation(OwnerVisible.class) != null,
                readAuthority != null ? List.of(readAuthority.value()) : List.of());
    }

    boolean isOpen() {
        return preAuthorize == null && authorities.isEmpty() && !ownerVisible;
    }
}
