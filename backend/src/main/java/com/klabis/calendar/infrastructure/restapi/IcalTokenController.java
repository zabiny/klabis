package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.users.UserId;
import com.klabis.members.ActingUser;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
class IcalTokenController implements IcalTokenApi {

    private final IcalTokenPort icalTokenPort;
    private final String baseUrl;

    IcalTokenController(
            IcalTokenPort icalTokenPort,
            @Value("${klabis.ical.base-url:https://localhost:8443}") String baseUrl) {
        this.icalTokenPort = icalTokenPort;
        this.baseUrl = baseUrl;
    }

    @Override
    public ResponseEntity<IcalTokenResponse> getTokenState(@ActingUser UserId currentUserId) {
        IcalTokenResponse response = icalTokenPort.getTokenState(currentUserId)
                .map(state -> IcalTokenResponseBuilder.builder()
                        .lastSetAt(state.lastSetAt())
                        .url(buildMaskedSubscribeUrl(state.tokenLookup()))
                        .build())
                .orElseGet(() -> IcalTokenResponseBuilder.builder().build());
        HalResponseContext.setDomain(response);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<IcalTokenResponse> generateToken(@ActingUser UserId currentUserId) {
        IcalTokenPort.GenerateResult result = icalTokenPort.generateOrRotate(currentUserId);
        IcalTokenResponse response = IcalTokenResponseBuilder.builder()
                .lastSetAt(result.lastSetAt())
                .url(buildSubscribeUrl(result.rawToken()))
                .build();
        HalResponseContext.setDomain(response);
        return ResponseEntity.ok(response);
    }

    private String buildSubscribeUrl(String rawToken) {
        return baseUrl + "/ical/my-schedule.ics?token=" + rawToken;
    }

    /** Masked URL — token is hashed in DB; raw token is never recoverable after generation. */
    private String buildMaskedSubscribeUrl(String tokenLookup) {
        // tokenLookup is the first 8 chars of the raw token (non-secret prefix used for indexed lookup).
        // The remaining characters are unknown — we mask the entire token portion.
        return baseUrl + "/ical/my-schedule.ics?token=••••••••••••••••••••••••••••••••••••••••";
    }
}

@MvcComponent
class IcalTokenResponsePostprocessor implements RepresentationModelProcessor<EntityModel<IcalTokenResponse>> {

    @Override
    public EntityModel<IcalTokenResponse> process(EntityModel<IcalTokenResponse> model) {
        klabisLinkTo(methodOn(IcalTokenApi.class).getTokenState(null))
                .ifPresent(link -> model.add(
                        link.withSelfRel()
                                .andAffordances(klabisAfford(methodOn(IcalTokenApi.class).generateToken(null)))
                ));
        return model;
    }
}
