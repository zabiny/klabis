package com.klabis.members;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public record CurrentUserData(@NonNull String userName, @NonNull UserId userId, @Nullable MemberId memberId, @NonNull Set<Authority> authorities) {
    public boolean isMember() {
        return memberId() != null;
    }

    public boolean hasAuthority(Authority authority) {
        return authorities.contains(authority);
    }
}
