package com.klabis.common.security.fieldsecurity;

import java.lang.annotation.*;

/**
 * Authorities of which at least one lets a user see a request field that they may not change.
 * <p>
 * The authorities guarding the change are given by {@link com.klabis.common.users.HasAuthority} and
 * {@link OwnerVisible}. A HAL-FORMS template shows such a field read-only for a user who holds one of these
 * authorities, and not at all for a user who holds none of them.
 */
@Target({ElementType.METHOD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReadAuthority {

    com.klabis.common.users.Authority[] value();
}
