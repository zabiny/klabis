package club.klabis.events.application;

import club.klabis.events.domain.Competition;
import club.klabis.events.domain.Event;
import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;
import club.klabis.shared.config.hateoas.KlabisInputTypes;
import club.klabis.shared.config.restapi.ResponseViews;
import com.fasterxml.jackson.annotation.JsonView;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.InputType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Set;

@RecordBuilder
public record EventManagementForm(@NotBlank String name, String location,
                                  @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                  String organizer,
                                  @InputType(KlabisInputTypes.DATE_TIME_INPUT_TYPE) @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime registrationDeadline,
                                  MemberId coordinator,
                                  @JsonView(ResponseViews.Detailed.class) Set<Competition.Category> categories,
                                  BigDecimal cost) {

    public Competition createNew() {
        Competition result = Competition.newEvent(name, date, categories);
        result.setLocation(location);
        result.setOrganizer(organizer);
        result.setCoordinator(coordinator);
        result.setRegistrationDeadline(registrationDeadline);
        return result;
    }

    public static EventManagementForm fromEvent(Event event) {
        Set<Competition.Category> categories = event instanceof Competition competition
                ? competition.getCategories()
                : null;

        return new EventManagementForm(
                event.getName(),
                event.getLocation(),
                event.getDate(),
                event.getOrganizer(),
                event.getRegistrationDeadline(),
                event.getCoordinator().orElse(null),
                categories,
                event.getCost().map(MoneyAmount::amount).orElse(null)
        );
    }

    public <T extends Event> T apply(T event) {
        event.setName(name);
        event.setLocation(location);
        event.setOrganizer(organizer);
        event.setCoordinator(coordinator);
        event.setEventDate(date);
        event.setRegistrationDeadline(registrationDeadline);
        if (cost != null) {
            event.updateCost(MoneyAmount.of(cost));
        }
        if (event instanceof Competition competition) {
            competition.setCategories(categories);
        }
        return event;
    }

}
