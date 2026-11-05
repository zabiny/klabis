package com.klabis.events;

import com.klabis.common.exceptions.UserFacingError;
import com.klabis.events.application.CategoryPresetNotFoundException;
import com.klabis.events.application.CoordinatorNotFoundException;
import com.klabis.events.application.EventNotFoundException;
import com.klabis.events.application.EventSyncNeedsResolutionException;
import com.klabis.events.application.MemberRegistrationBlockedException;
import com.klabis.events.domain.CategoryNotAvailableException;
import com.klabis.events.domain.CategoryRequiredException;
import com.klabis.events.domain.DisciplineNotArchivedException;
import com.klabis.events.domain.DisciplineNotEditableException;
import com.klabis.events.domain.DisciplineNotFoundException;
import com.klabis.events.domain.DuplicateCategoryNameException;
import com.klabis.events.domain.DuplicateCategoryOrisIdException;
import com.klabis.events.domain.DuplicateRegistrationException;
import com.klabis.events.domain.EventNotUpdatableException;
import com.klabis.events.domain.EventStatus;
import com.klabis.events.domain.EventTypeInUseException;
import com.klabis.events.domain.EventTypeNameAlreadyExistsException;
import com.klabis.events.domain.EventTypeNotFoundException;
import com.klabis.events.domain.InvalidEventStatusTransitionException;
import com.klabis.events.domain.OrisDisciplineAlreadyMappedException;
import com.klabis.events.domain.RegistrationDeadlineAfterEventDateException;
import com.klabis.events.domain.RegistrationNotFoundException;
import com.klabis.events.domain.RegistrationNotOpenException;
import com.klabis.events.domain.SiCardNumberRequiredException;
import com.klabis.events.domain.SiCardRentalNotAllowedException;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Events error types")
class EventsErrorTypesTest {

    private static final EventId EVENT_ID = new EventId(UUID.randomUUID());
    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());
    private static final DisciplineId DISCIPLINE_ID = new DisciplineId(UUID.randomUUID());
    private static final String CREATE_EVENT_REQUEST = "com.klabis.events.infrastructure.restapi.CreateEventRequest";
    private static final String REGISTER_EVENT_REQUEST = "com.klabis.events.infrastructure.restapi.RegisterEventRequest";
    private static final String EDIT_REGISTRATION_REQUEST = "com.klabis.events.infrastructure.restapi.EditRegistrationRequest";

    private static void assertError(UserFacingError error, String type, String field, Map<String, Object> parameters) {
        assertThat(error.errorType()).isEqualTo(type);
        assertThat(error.errorTypes()).contains(type);
        assertThat(error.field()).isEqualTo(field);
        assertThat(error.messageParameters()).containsExactlyInAnyOrderEntriesOf(parameters);
        String values = error.messageParameters().values().toString();
        assertThat(values).doesNotContain(EVENT_ID.value().toString(), MEMBER_ID.uuid().toString(),
                DISCIPLINE_ID.value().toString());
    }

    @Test
    @DisplayName("not found errors carry no identifier, ORIS lookups carry the ORIS id")
    void notFound() {
        assertError(new EventNotFoundException(EVENT_ID), "event-not-found", null, Map.of());
        assertError(new EventNotFoundException(8123), "oris-event-not-found", null, Map.of("orisId", 8123));
        assertError(new DisciplineNotFoundException(DISCIPLINE_ID), "discipline-not-found", null, Map.of());
        assertError(new DisciplineNotFoundException("2"), "oris-discipline-not-found", null, Map.of("orisId", "2"));
        assertError(new EventTypeNotFoundException(new EventTypeId(UUID.randomUUID())), "event-type-not-found", null,
                Map.of());
        assertError(new CategoryPresetNotFoundException(new CategoryPresetId(UUID.randomUUID())),
                "category-preset-not-found", null, Map.of());
        assertError(new RegistrationNotFoundException(MEMBER_ID, EVENT_ID), "registration-not-found", null, Map.of());
        assertError(new CoordinatorNotFoundException(List.of(MEMBER_ID)), "coordinator-not-found", null, Map.of());
    }

    @Test
    @DisplayName("conflicts name the event, the discipline or the event type")
    void conflicts() {
        assertError(new DuplicateRegistrationException(MEMBER_ID, EVENT_ID, "Jarní závod"),
                "duplicate-registration", null, Map.of("eventName", "Jarní závod"));
        assertError(new DisciplineNotEditableException("Sprint"), "discipline-not-editable", null,
                Map.of("disciplineName", "Sprint"));
        assertError(new DisciplineNotArchivedException("Sprint"), "discipline-not-archived", null,
                Map.of("disciplineName", "Sprint"));
        assertError(new OrisDisciplineAlreadyMappedException("Závod"), "oris-discipline-already-mapped", null,
                Map.of("eventTypeName", "Závod"));
        assertError(new OrisDisciplineAlreadyMappedException(), "oris-discipline-already-mapped", null, Map.of());
        assertError(new EventTypeInUseException(new EventTypeId(UUID.randomUUID()), List.of("A", "B")),
                "event-type-in-use", null, Map.of("eventNames", List.of("A", "B")));
        assertError(new EventTypeNameAlreadyExistsException("Trénink"), "event-type-name-already-exists", "name",
                Map.of("name", "Trénink"));
        assertError(new EventSyncNeedsResolutionException(8123), "event-sync-needs-resolution", null,
                Map.of("orisId", 8123));
    }

    @Test
    @DisplayName("business rules")
    void businessRules() {
        assertError(new MemberRegistrationBlockedException(MEMBER_ID), "member-registration-blocked", null, Map.of());
        assertError(new DuplicateCategoryNameException("M21"), "duplicate-category-name", null,
                Map.of("categoryName", "M21"));
        assertError(new DuplicateCategoryOrisIdException(), "duplicate-category-oris-id", null, Map.of());
        assertError(new RegistrationDeadlineAfterEventDateException(), "registration-deadline-after-event-date",
                "deadlines", Map.of());
        assertError(EventNotUpdatableException.finished(), "finished-event-cannot-be-updated", null, Map.of());
        assertError(EventNotUpdatableException.cancelled(), "cancelled-event-cannot-be-updated", null, Map.of());
        assertError(new CategoryRequiredException(), "category-required", "categoryId", Map.of());
        assertError(new CategoryNotAvailableException(), "category-not-available", "categoryId", Map.of());
        assertError(RegistrationNotOpenException.eventNotActive(), "event-not-active-for-registration", null, Map.of());
        assertError(RegistrationNotOpenException.deadlinePassed(), "registration-deadline-passed", null, Map.of());
        assertError(RegistrationNotOpenException.eventStarted(), "registration-closed-after-event-date", null,
                Map.of());
        assertError(new SiCardRentalNotAllowedException(), "si-card-rental-not-allowed", "wantsToRentSi", Map.of());
        assertError(new SiCardNumberRequiredException(), "si-card-number-required", "siCardNumber", Map.of());
        assertError(new InvalidEventStatusTransitionException(EventStatus.DRAFT, EventStatus.FINISHED),
                "invalid-event-status-transition", null, Map.of("from", "DRAFT", "to", "FINISHED"));
    }

    @Test
    @DisplayName("bound fields exist as properties of the request they belong to")
    void boundFieldsAreRequestProperties() throws Exception {
        assertThat(recordComponents(CREATE_EVENT_REQUEST)).contains("deadlines");
        assertThat(recordComponents(REGISTER_EVENT_REQUEST)).contains("categoryId", "wantsToRentSi", "siCardNumber");
        assertThat(recordComponents(EDIT_REGISTRATION_REQUEST)).contains("categoryId", "wantsToRentSi", "siCardNumber");
    }

    private static Set<String> recordComponents(String className) throws ClassNotFoundException {
        return Set.copyOf(Arrays.stream(Class.forName(className).getRecordComponents())
                .map(RecordComponent::getName).toList());
    }
}
