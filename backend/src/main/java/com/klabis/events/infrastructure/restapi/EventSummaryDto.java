package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.security.fieldsecurity.NullDeniedHandler;
import com.klabis.common.ui.HalForms;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.events.EventId;
import com.klabis.events.domain.EventStatus;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for event summary in list responses.
 * <p>
 * Contains essential event information for list views.
 * The status field is only visible to users with EVENTS:MANAGE authority.
 *
 * @param id                   event unique identifier
 * @param name                 event name
 * @param eventDate            event date
 * @param location             event location
 * @param organizer            event organizer
 * @param websiteUrl           event website URL (optional)
 * @param registrationDeadline registration deadline (optional)
 * @param status               event status — only visible to EVENTS:MANAGE holders
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
record EventSummaryDto(
        @HalForms(access = HalForms.Access.READ_ONLY) EventId id,
        String name,
        LocalDate eventDate,
        String location,
        String organizer,
        String websiteUrl,
        LocalDate registrationDeadline,
        @HasAuthority(Authority.EVENTS_MANAGE)
        @HalForms(access = HalForms.Access.READ_ONLY) EventStatus status,
        List<String> categories
) {
}
