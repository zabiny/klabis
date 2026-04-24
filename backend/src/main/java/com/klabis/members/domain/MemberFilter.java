package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record MemberFilter(StatusFilter status, String fulltextQuery) {

    public enum StatusFilter {
        ACTIVE, INACTIVE, ALL
    }

    public MemberFilter {
        if (fulltextQuery != null) {
            fulltextQuery = fulltextQuery.trim().isEmpty() ? null : fulltextQuery.trim();
        }
    }

    public static MemberFilter all() {
        return new MemberFilter(StatusFilter.ALL, null);
    }

    public static MemberFilter activeOnly() {
        return new MemberFilter(StatusFilter.ACTIVE, null);
    }

    public MemberFilter withFulltext(String query) {
        return new MemberFilter(status, query);
    }

    public MemberFilter withStatus(StatusFilter statusFilter) {
        return new MemberFilter(statusFilter, fulltextQuery);
    }
}
