package com.klabis.notifications;

import org.jspecify.annotations.Nullable;

public record PushMessage(String title, String body, @Nullable String targetPath) {
}
