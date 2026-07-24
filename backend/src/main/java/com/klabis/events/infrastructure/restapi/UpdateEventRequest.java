package com.klabis.events.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.events.EventTypeId;
import com.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.URL;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashSet;
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

        PatchField<LinkedHashSet<MemberId>> coordinators,

        PatchField<EventTypeId> eventTypeId,

        @Size(max = 3, message = "At most 3 deadlines are allowed")
        PatchField<List<LocalDate>> deadlines,

        PatchField<List<String>> categories,

        PatchField<RankingRequest> ranking,

        PatchField<EntryFeeRequest> baseEntryFee
) {

    record RankingRequest(
            @Positive(message = "Ranking levelId must be positive")
            int levelId,

            @NotBlank(message = "Ranking shortName is required")
            String shortName,

            @NotBlank(message = "Ranking name is required")
            String name
    ) {}

    record EntryFeeRequest(
            @NotNull(message = "Entry fee amount is required")
            @DecimalMin(value = "0", message = "Entry fee amount must be non-negative")
            BigDecimal amount,

            @NotBlank(message = "Entry fee currency is required")
            @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
            String currency
    ) {}

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
