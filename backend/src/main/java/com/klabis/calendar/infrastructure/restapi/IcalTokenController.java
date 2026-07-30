package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.mvc.MvcComponent;
import com.klabis.common.ui.HalResponseContext;
import com.klabis.common.users.UserId;
import com.klabis.members.ActingUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Calendar Feed Token", description = "iCalendar feed token management for the current user")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class IcalTokenController implements IcalTokenApi {

    private final IcalTokenPort icalTokenPort;
    private final String baseUrl;

    IcalTokenController(
            IcalTokenPort icalTokenPort,
            @Value("${klabis.ical.base-url:https://localhost:8443}") String baseUrl) {
        this.icalTokenPort = icalTokenPort;
        this.baseUrl = baseUrl;
    }

    @Operation(
            summary = "Get iCal feed token state",
            description = """
                    Returns the current iCal feed token state for the authenticated user.
                    If a token exists, returns the masked subscribe URL and the timestamp when the token was last set.
                    The URL is masked — the full raw token is never stored and can only be revealed at generation time.
                    Use the 'regenerate' affordance (POST) to create or rotate the token and receive the full URL once.
                    If no token exists, returns url=null.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Token state returned")
    @Override
    public ResponseEntity<IcalTokenResponse> getTokenState(@ActingUser UserId currentUserId) {
        IcalTokenResponse response = icalTokenPort.getTokenState(currentUserId)
                .map(state -> new IcalTokenResponse(buildMaskedSubscribeUrl(state.tokenLookup()), state.lastSetAt()))
                .orElseGet(() -> new IcalTokenResponse(null, null));
        HalResponseContext.setDomain(response);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Generate or regenerate iCal feed token",
            description = """
                    Generates a new iCal feed token for the authenticated user, or regenerates (rotates) it if one
                    already exists. The previous subscribe URL immediately stops working.
                    Returns the full subscribe URL exactly once — it cannot be recovered afterwards.
                    Store it securely and add it to your calendar application.
                    """
    )
    @ApiResponse(responseCode = "200", description = "New token generated; full subscribe URL returned once")
    @Override
    public ResponseEntity<IcalTokenResponse> generateToken(@ActingUser UserId currentUserId) {
        IcalTokenPort.GenerateResult result = icalTokenPort.generateOrRotate(currentUserId);
        IcalTokenResponse response = new IcalTokenResponse(buildSubscribeUrl(result.rawToken()), result.lastSetAt());
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

record IcalTokenResponse(String url, Instant lastSetAt) {}

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
