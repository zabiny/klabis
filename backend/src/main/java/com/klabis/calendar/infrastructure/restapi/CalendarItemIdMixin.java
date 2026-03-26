package com.klabis.calendar.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.klabis.calendar.CalendarItemId;
import org.springframework.boot.jackson.JacksonMixin;

import java.util.UUID;

@JacksonMixin(CalendarItemId.class)
public abstract class CalendarItemIdMixin {

    @JsonValue
    abstract UUID value();

    @JsonCreator
    static CalendarItemId create(UUID value) {
        return new CalendarItemId(value);
    }
}
