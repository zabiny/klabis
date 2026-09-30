package com.klabis.members;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record CurrentUserData(@NonNull String userName, @NonNull UserId userId, @Nullable MemberId memberId, @NonNull Set<Authority> authorities) {
    public static Optional<CurrentUserData> from(@Nullable Authentication authentication) {
        if (!(authentication instanceof KlabisJwtAuthenticationToken token)) {
            return Optional.empty();
        }
        Set<Authority> authorities = token.getAuthorities().stream()
                .map(a -> Authority.fromString(a.getAuthority()))
                .collect(Collectors.toSet());
        return Optional.of(new CurrentUserData(token.getUsername(), token.getUserId(),
                token.getMemberIdUuid().map(MemberId::new).orElse(null), authorities));
    }

    public boolean isMember() {
        return memberId() != null;
    }

    public boolean hasAuthority(Authority authority) {
        return authorities.contains(authority);
    }

    public boolean isMemberOf(java.util.function.Predicate<MemberId> membershipCheck) {
        return isMember() && membershipCheck.test(memberId());
    }
}
