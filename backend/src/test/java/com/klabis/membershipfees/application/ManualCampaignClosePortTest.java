package com.klabis.membershipfees.application;

import com.klabis.members.MemberId;
import com.klabis.members.application.AllMembersPort;
import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.membershipfees.domain.YearlyFeeChargeMarkerRepository;
import com.klabis.finance.application.ChargePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManualCampaignClosePort")
class ManualCampaignClosePortTest {

    private static final FeeSelectionCampaignId CAMPAIGN_ID = new FeeSelectionCampaignId(UUID.randomUUID());
    private static final LocalDate DEADLINE_IN_FUTURE = LocalDate.of(2099, 12, 31);
    private static final MemberId MEMBER_ID = new MemberId(UUID.randomUUID());

    @Mock
    private FeeSelectionCampaignRepository campaignRepository;
    @Mock
    private AllMembersPort allMembersPort;
    @Mock
    private MembershipFeeGroupRepository groupRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ChargePort chargePort;
    @Mock
    private YearlyFeeChargeMarkerRepository markerRepository;

    private ManualCampaignClosePortImpl testedInstance;

    @BeforeEach
    void setUp() {
        CampaignProcessor campaignProcessor = new CampaignProcessor(
                groupRepository, eventPublisher, chargePort, markerRepository);
        testedInstance = new ManualCampaignClosePortImpl(campaignRepository, allMembersPort, campaignProcessor);
    }

    @Nested
    @DisplayName("when campaign exists and has not been processed yet")
    class WhenCampaignIsActive {

        private FeeSelectionCampaign activeCampaign;

        @BeforeEach
        void setUp() {
            activeCampaign = FeeSelectionCampaign.reconstruct(
                    CAMPAIGN_ID,
                    2099,
                    DEADLINE_IN_FUTURE,
                    null,
                    List.of());
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(activeCampaign));
            when(allMembersPort.findAll()).thenReturn(Set.of(MEMBER_ID));
            when(groupRepository.findByYear(2099)).thenReturn(List.of());
        }

        @Test
        @DisplayName("should call processPublication with the loaded campaign and all members")
        void shouldProcessPublication() {
            testedInstance.closeCampaign(CAMPAIGN_ID);

            verify(campaignRepository).save(any(FeeSelectionCampaign.class));
        }

        @Test
        @DisplayName("should mark the campaign as processed after closing")
        void shouldMarkCampaignAsProcessed() {
            testedInstance.closeCampaign(CAMPAIGN_ID);

            verify(campaignRepository).save(argThat(saved -> saved.getDeadlineProcessedAt() != null));
        }

        @Test
        @DisplayName("should fetch all members and pass them to processPublication")
        void shouldFetchAllMembers() {
            testedInstance.closeCampaign(CAMPAIGN_ID);

            verify(allMembersPort).findAll();
        }
    }

    @Nested
    @DisplayName("when campaign does not exist")
    class WhenCampaignNotFound {

        @Test
        @DisplayName("should throw FeeSelectionCampaignNotFoundException")
        void shouldThrowWhenCampaignNotFound() {
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> testedInstance.closeCampaign(CAMPAIGN_ID))
                    .isInstanceOf(FeeSelectionCampaignNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("when campaign was already processed")
    class WhenCampaignAlreadyProcessed {

        @Test
        @DisplayName("should throw CampaignAlreadyProcessedException before calling processPublication")
        void shouldThrowWhenAlreadyProcessed() {
            FeeSelectionCampaign processedCampaign = FeeSelectionCampaign.reconstruct(
                    CAMPAIGN_ID,
                    2020,
                    LocalDate.of(2020, 3, 31),
                    Instant.parse("2020-04-01T00:00:00Z"),
                    List.of());
            when(campaignRepository.findById(CAMPAIGN_ID)).thenReturn(Optional.of(processedCampaign));

            assertThatThrownBy(() -> testedInstance.closeCampaign(CAMPAIGN_ID))
                    .isInstanceOf(CampaignAlreadyProcessedException.class);

            verify(allMembersPort, never()).findAll();
        }
    }
}
