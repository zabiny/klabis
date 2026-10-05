package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.users.Authority;
import com.klabis.groups.infrastructure.restapi.DelegatedAuthority;
import org.springframework.lang.Nullable;

import java.util.EnumSet;
import java.util.Set;

final class DelegatedAuthorities {

    private DelegatedAuthorities() {
    }

    /**
     * Absent means the founder did not opt in: nothing is delegated.
     */
    static Set<Authority> toDomain(@Nullable Set<DelegatedAuthority> wire) {
        Set<Authority> domain = EnumSet.noneOf(Authority.class);
        if (wire != null) {
            wire.forEach(authority -> domain.add(Authority.fromString(authority.getValue())));
        }
        return domain;
    }

    static Set<DelegatedAuthority> toWire(Set<Authority> domain) {
        Set<DelegatedAuthority> wire = EnumSet.noneOf(DelegatedAuthority.class);
        domain.forEach(authority -> wire.add(DelegatedAuthority.fromValue(authority.getValue())));
        return wire;
    }
}
