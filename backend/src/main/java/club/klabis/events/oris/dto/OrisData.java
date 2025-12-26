package club.klabis.events.oris.dto;

import club.klabis.events.domain.OrisId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;

@RecordBuilder
public record OrisData(OrisId orisId, @NotBlank String name, @NotNull LocalDate eventDate,
                       @NotNull ZonedDateTime registrationsDeadline,
                       String location, String organizer, @NotNull Collection<String> categories, URL website,
                       @NotNull Collection<MemberRegistration> registrations) {

    public OrisData {
        if (registrations == null) {
            registrations = new ArrayList<>();
        }
        if (categories == null) {
            categories = new ArrayList<>();
        }
    }

    public record MemberRegistration(String memberRegistration, String category, String siCard, BigDecimal fee) {

    }

}
