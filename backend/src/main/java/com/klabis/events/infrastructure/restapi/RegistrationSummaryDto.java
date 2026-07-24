package com.klabis.events.infrastructure.restapi;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.klabis.common.security.fieldsecurity.NullDeniedHandler;
import com.klabis.common.security.fieldsecurity.OwnerId;
import com.klabis.common.security.fieldsecurity.OwnerVisible;
import com.klabis.common.users.Authority;
import com.klabis.common.users.HasAuthority;
import com.klabis.members.MemberId;
import org.springframework.hateoas.server.core.Relation;
import org.springframework.security.authorization.method.HandleAuthorizationDenied;

import java.time.Instant;
import java.util.Set;

@Relation(collectionRelation = "registrationDtoList")
@JsonInclude(JsonInclude.Include.NON_NULL)
@HandleAuthorizationDenied(handlerClass = NullDeniedHandler.class)
public record RegistrationSummaryDto(
        String firstName,
        String lastName,
        String category,
        @OwnerVisible
        @HasAuthority(Authority.EVENTS_REGISTRATIONS)
        Instant registrationTime,
        @JsonIgnore @OwnerId Set<MemberId> coordinators,
        @JsonIgnore MemberId registeredMemberId
) {
}
