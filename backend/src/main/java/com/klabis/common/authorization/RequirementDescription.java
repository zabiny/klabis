package com.klabis.common.authorization;

import com.klabis.common.users.Authority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

final class RequirementDescription {

    private RequirementDescription() {
    }

    static String of(List<Authority> authorities, Collection<TargetType> targets, boolean ownerVisible) {
        List<String> alternatives = new ArrayList<>();
        if (!authorities.isEmpty()) {
            String names = authorities.stream().map(Authority::getValue).collect(Collectors.joining(" or "));
            alternatives.add(!targets.isEmpty()
                    ? "authority %s (over everything or over the %s)".formatted(names,
                    targets.stream().map(Object::toString).collect(Collectors.joining(" or ")))
                    : "authority " + names);
        }
        if (ownerVisible) {
            alternatives.add("being the owner");
        }
        return String.join(", or ", alternatives);
    }
}
