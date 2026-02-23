package com.klabis.common.security;

import com.klabis.common.users.UserId;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

public record CurrentUserData(@NonNull String userName, @NonNull UserId userId, @Nullable UUID memberId) {
}
