package com.klabis.events.infrastructure.sync;

import com.klabis.events.EventCategory;
import com.klabis.events.domain.Event;
import com.klabis.events.domain.EventCreateEventFromOrisBuilder;
import com.klabis.events.domain.SiCardNumber;
import com.klabis.members.MemberId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(OutputCaptureExtension.class)
@DisplayName("CategoryRegistrationGuard")
class CategoryRegistrationGuardTest {

    private final EventCategory m21 = EventCategory.createFromOris("100", "M21");
    private final EventCategory w21 = EventCategory.createFromOris("200", "W21");

    private CategoryRegistrationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new CategoryRegistrationGuard();
    }

    private Event eventWithCategories() {
        return Event.createFromOris(EventCreateEventFromOrisBuilder.builder()
                .orisId(9090)
                .name("Race")
                .eventDate(LocalDate.now().plusDays(30))
                .location("Forest")
                .organizer("OOB")
                .categories(List.of(m21, w21))
                .build());
    }

    @Test
    @DisplayName("warns when an incoming ORIS sync drops a category that still has registrations")
    void warnsWhenRegisteredCategoryDropped(CapturedOutput output) {
        Event event = eventWithCategories();
        event.publish();
        event.registerMember(new MemberId(UUID.randomUUID()), new SiCardNumber("12345"), m21.id());

        guard.warnIfSyncRemovesCategoriesWithRegistrations(event, List.of(w21));

        assertThat(output).contains("will remove categories that have existing registrations");
        assertThat(output).contains("M21");
    }

    @Test
    @DisplayName("does not warn when the registered category is kept")
    void doesNotWarnWhenRegisteredCategoryKept(CapturedOutput output) {
        Event event = eventWithCategories();
        event.publish();
        event.registerMember(new MemberId(UUID.randomUUID()), new SiCardNumber("12345"), m21.id());

        guard.warnIfSyncRemovesCategoriesWithRegistrations(event, List.of(m21, w21));

        assertThat(output).doesNotContain("will remove categories that have existing registrations");
    }

    @Test
    @DisplayName("no-op when the event has no registrations")
    void noOpWithoutRegistrations(CapturedOutput output) {
        Event event = eventWithCategories();

        assertThatCode(() -> guard.warnIfSyncRemovesCategoriesWithRegistrations(event, List.of()))
                .doesNotThrowAnyException();
        assertThat(output).doesNotContain("will remove categories that have existing registrations");
    }
}
