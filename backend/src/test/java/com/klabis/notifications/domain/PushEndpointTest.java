package com.klabis.notifications.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PushEndpoint")
class PushEndpointTest {

    @Test
    @DisplayName("accepts https URL")
    void acceptsHttps() {
        assertThat(new PushEndpoint("https://fcm.googleapis.com/fcm/send/abc").url())
                .isEqualTo("https://fcm.googleapis.com/fcm/send/abc");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "http://push.example.org/send/1", "ftp://push.example.org/1", "push.example.org/1",
            "https://", "https://exa mple.org/1"})
    @DisplayName("rejects blank, non-https and malformed URLs")
    void rejectsInvalid(String url) {
        assertThatThrownBy(() -> new PushEndpoint(url)).isInstanceOf(IllegalArgumentException.class);
    }
}
