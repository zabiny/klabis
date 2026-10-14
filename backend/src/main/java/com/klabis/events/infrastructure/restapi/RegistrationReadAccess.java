package com.klabis.events.infrastructure.restapi;

import com.klabis.common.authorization.AuthorizationEvaluator;
import com.klabis.common.authorization.TargetRef;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.users.Authority;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Single rule for reading a registration detail: the registered member, or a holder of
 * EVENTS:REGISTRATIONS / EVENTS:REGISTRATIONS_READ over the event. {@code getRegistration} is not
 * declared in the spec because one target cannot express "owner of the member or grant over the
 * event", so this check must guard the endpoint and every link to it.
 */
@MvcComponent
class RegistrationReadAccess {

    private static final List<Authority> READ_AUTHORITIES =
            List.of(Authority.EVENTS_REGISTRATIONS, Authority.EVENTS_REGISTRATIONS_READ);

    private final AuthorizationEvaluator authorizationEvaluator;

    RegistrationReadAccess(AuthorizationEvaluator authorizationEvaluator) {
        this.authorizationEvaluator = authorizationEvaluator;
    }

    boolean canRead(UUID eventId, UUID memberId) {
        return authorizationEvaluator.isSelf(TargetRef.member(memberId)) || canReadRegistrations(eventId);
    }

    boolean canReadRegistrations(UUID eventId) {
        return authorizationEvaluator.isAllowed(READ_AUTHORITIES, TargetRef.event(eventId), false);
    }

    Optional<WebMvcLinkBuilder> linkTo(UUID eventId, UUID memberId) {
        if (!canRead(eventId, memberId)) {
            return Optional.empty();
        }
        return klabisLinkTo(methodOn(EventRegistrationsApi.class).getRegistration(memberId, eventId, false));
    }
}
