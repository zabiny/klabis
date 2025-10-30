package club.klabis.events.infrastructure.restapi.dto;

import club.klabis.members.MemberId;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.springframework.hateoas.server.core.Relation;

@RecordBuilder
@JsonTypeName("EventRegistration")
@Relation("eventRegistration")
public record EventRegistrationResponse(
        MemberId memberId,
        String category) {
}
