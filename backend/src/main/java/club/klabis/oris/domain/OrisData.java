package club.klabis.oris.domain;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.events.domain.OrisId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;

@RecordBuilder
public record OrisData(OrisId orisId, @NotBlank String name, @NotNull LocalDate eventDate,
                       @NotNull ZonedDateTime registrationsDeadline,
                       String location, String organizer, @NotNull Collection<String> categories, URL website) {

    public Competition createCompetition() {
        Competition result = new Competition(name, eventDate);
        this.apply(result);
        return result;
    }

    public Event apply(Event event) {
        // for initial import from ORIS
        event.linkWithOris(orisId);

        event.setName(name);
        event.setLocation(location);
        event.setOrganizer(organizer);
        event.setEventDate(eventDate);
        event.setRegistrationDeadline(registrationsDeadline);
        event.withWebsite(website);

        if (event instanceof Competition competition) {
            competition.setCategories(categories.stream().map(Competition.Category::new).toList());
        }

        return event;
    }

}
