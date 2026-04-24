package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record MemberFilter(boolean onlyActive, String fulltextQuery) {

    public MemberFilter {
        if (fulltextQuery != null) {
            fulltextQuery = fulltextQuery.trim().isEmpty() ? null : fulltextQuery.trim();
        }
    }

    public static MemberFilter all() {
        return new MemberFilter(false, null);
    }

    public static MemberFilter activeOnly() {
        return new MemberFilter(true, null);
    }

    public MemberFilter withFulltext(String query) {
        return new MemberFilter(onlyActive, query);
    }
}
