package com.klabis.events.infrastructure.restapi;

import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.security.KlabisJwtAuthenticationToken;
import com.klabis.common.ui.DashboardModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;

/**
 * The link points at the pre-built events-list query the frontend follows verbatim to
 * populate the "Moje nadcházející akce" dashboard widget. Presence of the link is the
 * signal to the frontend that the widget should be rendered; absence means the current
 * user has no member profile and therefore cannot have registrations.
 */
@MvcComponent
class DashboardUpcomingRegistrationsLinkProcessor implements RepresentationModelProcessor<EntityModel<DashboardModel>> {

    @Override
    public EntityModel<DashboardModel> process(EntityModel<DashboardModel> model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof KlabisJwtAuthenticationToken token) || !token.hasMemberProfile()) {
            return model;
        }

        String href = UriComponentsBuilder.fromPath("/api/events")
                .queryParam("registeredBy", "me")
                .queryParam("dateFrom", LocalDate.now())
                .queryParam("sort", "eventDate,ASC")
                .queryParam("size", 3)
                .build()
                .toUriString();

        model.add(Link.of(href, "upcomingRegistrations"));
        return model;
    }
}
