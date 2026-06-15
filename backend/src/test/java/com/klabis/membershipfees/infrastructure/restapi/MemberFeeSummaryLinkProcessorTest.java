package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.security.JwtParams;
import com.klabis.common.security.KlabisAuthenticationFactory;
import com.klabis.members.MemberId;
import com.klabis.members.MemberResource;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("MemberFeeSummaryLinkProcessor")
class MemberFeeSummaryLinkProcessorTest {

    private static final UUID MEMBER_UUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_MEMBER_UUID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final int CURRENT_YEAR = 2026;
    private static final int CAMPAIGN_YEAR = 2027;
    private final FeeSelectionCampaignManagementPort campaignPort = mock(FeeSelectionCampaignManagementPort.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), ZoneId.of("UTC"));
    private final MemberFeeSummaryLinkProcessor processor = new MemberFeeSummaryLinkProcessor(Optional.of(campaignPort), clock);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAsMember(UUID memberUuid) {
        var token = KlabisAuthenticationFactory.createAuthenticationToken(
                JwtParams.member(memberUuid)
        );
        SecurityContextHolder.getContext().setAuthentication(token);
    }

    private EntityModel<MemberResource> modelForMember(UUID memberId) {
        MemberResource resource = () -> new MemberId(memberId);
        return EntityModel.of(resource);
    }

    @Nested
    @DisplayName("when active campaign exists")
    class WithActiveCampaign {

        @BeforeEach
        void stubActiveCampaignYear() {
            when(campaignPort.relevantFeeYear(any(LocalDate.class))).thenReturn(CAMPAIGN_YEAR);
        }

        @Test
        @DisplayName("feeSummary link is added pointing to campaign year when viewing own profile")
        void feeSummaryLinkAddedWithCampaignYear() {
            authenticateAsMember(MEMBER_UUID);
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("feeSummary")).isPresent();
            assertThat(model.getLink("feeSummary").get().getHref())
                    .contains("/api/members/" + MEMBER_UUID + "/fee-summary/" + CAMPAIGN_YEAR);
        }

        @Test
        @DisplayName("feeSummary link is NOT added when viewing another member's profile")
        void feeSummaryLinkNotAddedForOtherMember() {
            authenticateAsMember(OTHER_MEMBER_UUID);
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("feeSummary")).isEmpty();
        }
    }

    @Nested
    @DisplayName("when no active campaign (fallback to current year)")
    class WithoutActiveCampaign {

        @BeforeEach
        void stubCurrentYearFallback() {
            when(campaignPort.relevantFeeYear(any(LocalDate.class))).thenReturn(CURRENT_YEAR);
        }

        @Test
        @DisplayName("feeSummary link is added pointing to current year")
        void feeSummaryLinkAddedWithCurrentYear() {
            authenticateAsMember(MEMBER_UUID);
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("feeSummary")).isPresent();
            assertThat(model.getLink("feeSummary").get().getHref())
                    .contains("/api/members/" + MEMBER_UUID + "/fee-summary/" + CURRENT_YEAR);
        }
    }

    @Nested
    @DisplayName("when user has no member profile (admin-only account)")
    class NoMemberProfile {

        @BeforeEach
        void authenticateAsAdminWithoutMemberProfile() {
            var token = KlabisAuthenticationFactory.createAuthenticationToken(
                    JwtParams.jwtTokenParams("ZBM0001", UUID.randomUUID())
            );
            SecurityContextHolder.getContext().setAuthentication(token);
        }

        @Test
        @DisplayName("feeSummary link is NOT added")
        void feeSummaryLinkNotAdded() {
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("feeSummary")).isEmpty();
        }
    }

    @Nested
    @DisplayName("when unauthenticated")
    class Unauthenticated {

        @Test
        @DisplayName("feeSummary link is NOT added")
        void feeSummaryLinkNotAdded() {
            SecurityContextHolder.clearContext();
            EntityModel<MemberResource> model = modelForMember(MEMBER_UUID);

            processor.process(model);

            assertThat(model.getLink("feeSummary")).isEmpty();
        }
    }
}
