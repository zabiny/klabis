package com.klabis.common.authorization;

import com.klabis.common.users.Authority;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class RequirementDescription {

    private RequirementDescription() {
    }

    static String of(List<Authority> authorities, @Nullable TargetType target, boolean ownerVisible) {
        List<String> alternatives = new ArrayList<>();
        if (!authorities.isEmpty()) {
            String names = authorities.stream().map(Authority::getValue).collect(Collectors.joining(" or "));
            alternatives.add(target != null
                    ? "authority %s (over everything or over the %s)".formatted(names, target)
                    : "authority " + names);
        }
        if (ownerVisible) {
            alternatives.add("being the owner");
        }
        return String.join(", or ", alternatives);
    }
}
