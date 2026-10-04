package com.klabis.members;

import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.users.UserId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;

import java.util.Optional;

public record CurrentUserData(@NonNull String userName, @NonNull UserId userId, @Nullable MemberId memberId) {
    public static Optional<CurrentUserData> from(@Nullable Authentication authentication) {
        if (!(authentication instanceof KlabisJwtAuthenticationToken token)) {
            return Optional.empty();
        }
        return Optional.of(new CurrentUserData(token.getUsername(), token.getUserId(),
                token.getMemberIdUuid().map(MemberId::new).orElse(null)));
    }

    public boolean isMember() {
        return memberId() != null;
    }

    public boolean isMemberOf(java.util.function.Predicate<MemberId> membershipCheck) {
        return isMember() && membershipCheck.test(memberId());
    }
}
