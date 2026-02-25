package com.klabis.members;

import com.klabis.common.users.UserId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public record CurrentUserData(@NonNull String userName, @NonNull UserId userId, @Nullable MemberId memberId) {
    public boolean isMember() {
        return memberId() != null;
    }
}
