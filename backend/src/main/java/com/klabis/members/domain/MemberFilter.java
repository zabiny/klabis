package com.klabis.members.domain;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;
import java.util.Set;

/**
 * @param bornOn restricts to members born on one of these dates; null means no restriction, empty matches nobody
 * @param bornOnOrBefore restricts to members born on or before this date; null means no restriction
 */
@ValueObject
public record MemberFilter(StatusFilter status, String fulltextQuery, boolean incompleteOnly, Set<LocalDate> bornOn,
                           LocalDate bornOnOrBefore) {

    public enum StatusFilter {
        ACTIVE, INACTIVE, ALL
    }

    public MemberFilter {
        if (fulltextQuery != null) {
            fulltextQuery = fulltextQuery.trim().isEmpty() ? null : fulltextQuery.trim();
        }
    }

    public MemberFilter(StatusFilter status, String fulltextQuery, boolean incompleteOnly) {
        this(status, fulltextQuery, incompleteOnly, null, null);
    }

    public static MemberFilter all() {
        return new MemberFilter(StatusFilter.ALL, null, false);
    }

    public static MemberFilter activeOnly() {
        return new MemberFilter(StatusFilter.ACTIVE, null, false);
    }

    public MemberFilter withFulltext(String query) {
        return new MemberFilter(status, query, incompleteOnly, bornOn, bornOnOrBefore);
    }

    public MemberFilter withStatus(StatusFilter statusFilter) {
        return new MemberFilter(statusFilter, fulltextQuery, incompleteOnly, bornOn, bornOnOrBefore);
    }

    public MemberFilter withIncompleteOnly(boolean incompleteOnly) {
        return new MemberFilter(status, fulltextQuery, incompleteOnly, bornOn, bornOnOrBefore);
    }

    public MemberFilter withBornOn(Set<LocalDate> bornOn) {
        return new MemberFilter(status, fulltextQuery, incompleteOnly, bornOn, bornOnOrBefore);
    }

    public MemberFilter withBornOnOrBefore(LocalDate bornOnOrBefore) {
        return new MemberFilter(status, fulltextQuery, incompleteOnly, bornOn, bornOnOrBefore);
    }
}
