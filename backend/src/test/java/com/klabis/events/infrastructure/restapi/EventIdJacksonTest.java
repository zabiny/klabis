package com.klabis.events.infrastructure.restapi;

import com.klabis.events.EventId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class EventIdJacksonTest {

    @Autowired
    private JacksonTester<EventId> json;

    @Test
    void shouldSerializeEventIdAsUuidString() throws Exception {
        UUID uuid = UUID.randomUUID();
        EventId eventId = new EventId(uuid);

        assertThat(json.write(eventId)).isEqualToJson("\"" + uuid + "\"");
    }

    @Test
    void shouldDeserializeEventIdFromUuidString() throws Exception {
        UUID uuid = UUID.randomUUID();

        assertThat(json.parse("\"" + uuid + "\"")).isEqualTo(new EventId(uuid));
    }
}
