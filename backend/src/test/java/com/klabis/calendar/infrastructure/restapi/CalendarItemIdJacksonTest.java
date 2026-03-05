package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.CalendarItemId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class CalendarItemIdJacksonTest {

    @Autowired
    private JacksonTester<CalendarItemId> json;

    @Test
    void shouldSerializeCalendarItemIdAsUuidString() throws Exception {
        UUID uuid = UUID.randomUUID();
        CalendarItemId calendarItemId = new CalendarItemId(uuid);

        assertThat(json.write(calendarItemId)).isEqualToJson("\"" + uuid + "\"");
    }

    @Test
    void shouldDeserializeCalendarItemIdFromUuidString() throws Exception {
        UUID uuid = UUID.randomUUID();

        assertThat(json.parse("\"" + uuid + "\"")).isEqualTo(new CalendarItemId(uuid));
    }
}
