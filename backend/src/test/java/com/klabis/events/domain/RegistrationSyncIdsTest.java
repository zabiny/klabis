package com.klabis.events.domain;

import com.klabis.events.EventId;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Registration sync id codecs")
class RegistrationSyncIdsTest {

    @Test
    @DisplayName("local id formats and parses back")
    void localRoundTrip() {
        RegistrationSyncId id = new RegistrationSyncId(EventId.generate(), new MemberId(java.util.UUID.randomUUID()));

        assertThat(RegistrationSyncId.parse(id.format())).isEqualTo(id);
        assertThat(id.format()).startsWith(RegistrationSyncId.prefixOf(id.eventId()));
    }

    @Test
    @DisplayName("ORIS id formats and parses back")
    void orisRoundTrip() {
        assertThat(new OrisRegistrationId(7001, 12).format()).isEqualTo("7001:12");
        assertThat(OrisRegistrationId.parse("7001:12")).isEqualTo(new OrisRegistrationId(7001, 12));
    }

    @Test
    @DisplayName("malformed ids fail with IllegalArgumentException, never an index error")
    void malformedIds() {
        for (String malformed : new String[]{"", "nocolon", ":", "abc:def", "1:", ":1"}) {
            assertThatThrownBy(() -> OrisRegistrationId.parse(malformed)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> RegistrationSyncId.parse(malformed)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> RegistrationSyncId.parse(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> OrisRegistrationId.parse(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
