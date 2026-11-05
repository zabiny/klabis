package com.klabis.common.groups.infrastructure.restapi;

import com.klabis.common.groups.domain.MemberNameLookup;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public final class MemberNameParameter {

    private static final Logger LOG = LoggerFactory.getLogger(MemberNameParameter.class);

    private MemberNameParameter() {
    }

    public static Map<String, String> of(@Nullable MemberNameLookup lookup, Object memberId) {
        if (lookup == null) {
            return Map.of();
        }
        try {
            return lookup.nameOf(memberId).map(name -> Map.of("memberName", name)).orElse(Map.of());
        } catch (RuntimeException e) {
            LOG.warn("Member name lookup for error message failed", e);
            return Map.of();
        }
    }
}
