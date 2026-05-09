package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.RegistrationDeadlines;
import com.klabis.members.MemberId;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating an event.
 * Accepts deadlines as a List&lt;LocalDate&gt; (max 3, min 1 if provided) and maps to domain RegistrationDeadlines.
 */
record CreateEventRequest(
        @NotBlank(message = "Event name is required")
        @Size(max = 100, message = "Event name must not exceed 100 characters")
        String name,

        @NotNull(message = "Event date is required")
        LocalDate eventDate,

        @Size(max = 100, message = "Event location must not exceed 100 characters")
        String location,

        @NotBlank(message = "Event organizer is required")
        @Size(max = 10, message = "Event organizer must not exceed 10 characters")
        String organizer,

        @URL(message = "Website URL must be valid")
        String websiteUrl,

        MemberId eventCoordinatorId,

        @Size(min = 1, max = 3, message = "Between 1 and 3 deadlines are allowed")
        List<LocalDate> deadlines,

        List<String> categories
) {

    @AssertTrue(message = "Deadlines must be in non-decreasing order")
    boolean isDeadlinesOrdered() {
        if (deadlines == null || deadlines.size() < 2) {
            return true;
        }
        for (int i = 1; i < deadlines.size(); i++) {
            if (deadlines.get(i).isBefore(deadlines.get(i - 1))) {
                return false;
            }
        }
        return true;
    }

    RegistrationDeadlines toRegistrationDeadlines() {
        if (deadlines == null || deadlines.isEmpty()) {
            return RegistrationDeadlines.none();
        }
        return RegistrationDeadlines.of(
                deadlines.get(0),
                deadlines.size() > 1 ? deadlines.get(1) : null,
                deadlines.size() > 2 ? deadlines.get(2) : null
        );
    }
}
