package com.klabis.events.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.events.EventTypeId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for partially updating an event via PATCH.
 * Any subset of fields may be provided; absent fields leave the corresponding event field unchanged.
 * Required-field constraints (@NotBlank, @Size) apply only when the field is present in the request body.
 */
@RecordBuilder
record UpdateEventRequest(
        @NotBlank(message = "Event name is required")
        @Size(max = 100, message = "Event name must not exceed 100 characters")
        PatchField<String> name,

        PatchField<LocalDate> eventDate,

        @Size(max = 100, message = "Event location must not exceed 100 characters")
        PatchField<String> location,

        @NotBlank(message = "Event organizer is required")
        @Size(max = 10, message = "Event organizer must not exceed 10 characters")
        PatchField<String> organizer,

        @URL(message = "Website URL must be valid")
        PatchField<String> websiteUrl,

        PatchField<MemberId> eventCoordinatorId,

        PatchField<EventTypeId> eventTypeId,

        @Size(min = 1, max = 3, message = "Between 1 and 3 deadlines are allowed")
        PatchField<List<LocalDate>> deadlines,

        PatchField<List<String>> categories
) {

    @AssertTrue(message = "Deadlines must be in non-decreasing order")
    boolean isDeadlinesOrdered() {
        if (!deadlines.isProvided()) {
            return true;
        }
        List<LocalDate> dates = deadlines.throwIfNotProvided();
        if (dates == null || dates.size() < 2) {
            return true;
        }
        for (int i = 1; i < dates.size(); i++) {
            if (dates.get(i).isBefore(dates.get(i - 1))) {
                return false;
            }
        }
        return true;
    }
}
