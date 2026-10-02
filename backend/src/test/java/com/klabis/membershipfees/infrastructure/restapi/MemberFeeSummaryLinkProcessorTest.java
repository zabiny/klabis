package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.members.infrastructure.restapi.MemberDetailsResponse;
import com.klabis.members.infrastructure.restapi.MemberDetailsResponseBuilder;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// Branches not reachable through a controller response (the endpoint answers 401 first) — the
// reachable behaviour is covered by MemberFeeSummaryControllerTest.FeeSummaryLinkOnMemberDetailTests.
@DisplayName("MemberFeeSummaryLinkProcessor")
class MemberFeeSummaryLinkProcessorTest {

    private final MemberFeeSummaryLinkProcessor processor = new MemberFeeSummaryLinkProcessor(
            mock(FeeSelectionCampaignManagementPort.class),
            Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), ZoneId.of("UTC")));

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("feeSummary link is NOT added when unauthenticated")
    void feeSummaryLinkNotAddedWhenUnauthenticated() {
        SecurityContextHolder.clearContext();
        EntityModel<MemberDetailsResponse> model = EntityModel.of(
                MemberDetailsResponseBuilder.builder().id(UUID.randomUUID()).build());

        processor.process(model);

        assertThat(model.getLink("feeSummary")).isEmpty();
    }
}
