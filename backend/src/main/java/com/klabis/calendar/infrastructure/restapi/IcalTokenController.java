package com.klabis.calendar.infrastructure.restapi;

import com.klabis.calendar.application.IcalTokenPort;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static com.klabis.common.ui.HalFormsSupport.klabisAfford;
import static com.klabis.common.ui.HalFormsSupport.klabisLinkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@PrimaryAdapter
@RestController
@RequestMapping(value = "/api/me/ical-token", produces = MediaTypes.HAL_FORMS_JSON_VALUE)
@Tag(name = "Calendar Feed Token", description = "iCalendar feed token management for the current user")
@SecurityRequirement(name = "KlabisAuth", scopes = {"openid"})
class IcalTokenController {

    private final IcalTokenPort icalTokenPort;
    private final String baseUrl;

    IcalTokenController(
            IcalTokenPort icalTokenPort,
            @Value("${klabis.ical.base-url:https://localhost:8443}") String baseUrl) {
        this.icalTokenPort = icalTokenPort;
        this.baseUrl = baseUrl;
    }

    @GetMapping
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
    ResponseEntity<EntityModel<IcalTokenResponse>> getTokenState(@ActingUser UserId currentUserId) {
        return icalTokenPort.getTokenState(currentUserId)
                .map(state -> {
                    String maskedUrl = buildMaskedSubscribeUrl(state.tokenLookup());
                    IcalTokenResponse response = new IcalTokenResponse(maskedUrl, state.lastSetAt());
                    return ResponseEntity.ok(buildModel(response));
                })
                .orElseGet(() -> {
                    IcalTokenResponse response = new IcalTokenResponse(null, null);
                    return ResponseEntity.ok(buildModel(response));
                });
    }

    @PostMapping
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
    ResponseEntity<EntityModel<IcalTokenResponse>> generateToken(@ActingUser UserId currentUserId) {
        IcalTokenPort.GenerateResult result = icalTokenPort.generateOrRotate(currentUserId);
        String fullUrl = buildSubscribeUrl(result.rawToken());
        IcalTokenResponse response = new IcalTokenResponse(fullUrl, result.lastSetAt());
        return ResponseEntity.ok(buildModel(response));
    }

    private EntityModel<IcalTokenResponse> buildModel(IcalTokenResponse response) {
        EntityModel<IcalTokenResponse> model = EntityModel.of(response);
        klabisLinkTo(methodOn(IcalTokenController.class).getTokenState(null))
                .ifPresent(link -> model.add(
                        link.withSelfRel()
                                .andAffordances(klabisAfford(methodOn(IcalTokenController.class).generateToken(null)))
                ));
        return model;
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
