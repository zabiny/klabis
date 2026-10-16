package com.klabis.notifications.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PushKeys")
class PushKeysTest {

    @Test
    @DisplayName("requires both p256dh and auth")
    void requiresBoth() {
        assertThatThrownBy(() -> new PushKeys(null, "auth")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PushKeys("p256dh", " ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("does not leak secrets in toString")
    void toStringHidesSecrets() {
        assertThat(new PushKeys("p256dh-secret", "auth-secret").toString())
                .doesNotContain("p256dh-secret")
                .doesNotContain("auth-secret");
    }
}
