package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record MemberFilter(boolean onlyActive) {

    public static MemberFilter all() {
        return new MemberFilter(false);
    }

    public static MemberFilter activeOnly() {
        return new MemberFilter(true);
    }
}
