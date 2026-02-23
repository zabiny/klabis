package com.klabis.common.ui;

import com.klabis.common.users.UserId;

import java.util.UUID;

public record RootModel(UserId userId, UUID memberId) {
}
