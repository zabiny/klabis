package com.klabis.events.infrastructure.sync;

import com.dpolach.api.orisclient.dto.EventDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrisEventDetailsMapper")
class OrisEventDetailsMapperTest {

    private final OrisEventDetailsMapper mapper = Mappers.getMapper(OrisEventDetailsMapper.class);

    @Test
    @DisplayName("maps name, date, place and currency field-to-field")
    void mapsTrivialFields() {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.when(details.name()).thenReturn("Spring Sprint");
        Mockito.when(details.date()).thenReturn(LocalDate.of(2026, 8, 15));
        Mockito.when(details.place()).thenReturn("Brno Park");
        Mockito.when(details.currency()).thenReturn("CZK");

        OrisEventDetailsMapper.TrivialEventFields result = mapper.toTrivialFields(details);

        assertThat(result.name()).isEqualTo("Spring Sprint");
        assertThat(result.eventDate()).isEqualTo(LocalDate.of(2026, 8, 15));
        assertThat(result.location()).isEqualTo("Brno Park");
        assertThat(result.currency()).isEqualTo("CZK");
    }

    @Test
    @DisplayName("passes through null scalar fields")
    void passesThroughNulls() {
        EventDetails details = Mockito.mock(EventDetails.class);
        Mockito.when(details.name()).thenReturn("Race");
        Mockito.when(details.date()).thenReturn(null);
        Mockito.when(details.place()).thenReturn(null);
        Mockito.when(details.currency()).thenReturn(null);

        OrisEventDetailsMapper.TrivialEventFields result = mapper.toTrivialFields(details);

        assertThat(result.name()).isEqualTo("Race");
        assertThat(result.eventDate()).isNull();
        assertThat(result.location()).isNull();
        assertThat(result.currency()).isNull();
    }
}
