package com.klabis.membershipfees.application;

import com.klabis.membershipfees.FeeSelectionCampaignId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.klabis.finance.domain.Money.ofCzk;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeeSelectionCampaignManagementService")
class FeeSelectionCampaignManagementServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);
    private static final int YEAR = 2026;

    @Mock
    private FeeSelectionCampaignRepository publicationRepository;
    @Mock
    private MembershipFeeGroupRepository groupRepository;
    @Mock
    private MembershipFeeTierRepository tierRepository;

    private FeeSelectionCampaignManagementService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-03-01T00:00:00Z"), ZoneId.of("UTC"));
        service = new FeeSelectionCampaignManagementService(
                publicationRepository, groupRepository, tierRepository, clock);
    }

    private MembershipFeeTier aTier() {
        return MembershipFeeTier.create("Dospělý", ofCzk(new BigDecimal("1200.00")));
    }

    private FeeSelectionCampaign anActiveCampaign() {
        LocalDate futureDeadline = TODAY.plusDays(30);
        return FeeSelectionCampaign.reconstruct(
                new FeeSelectionCampaignId(UUID.randomUUID()),
                YEAR, futureDeadline, null, List.of());
    }

    @Nested
    @DisplayName("publishYear()")
    class PublishYear {

        @Test
        @DisplayName("should throw DeadlineNotInFutureException when deadline is in the past")
        void shouldThrowWhenDeadlineInPast() {
            LocalDate pastDeadline = TODAY.minusDays(1);
            MembershipFeeTierId tierId = new MembershipFeeTierId(UUID.randomUUID());

            assertThatThrownBy(() -> service.publishYear(new FeeSelectionCampaignManagementPort.PublishYearCommand(
                    YEAR, pastDeadline, List.of(tierId))))
                    .isInstanceOf(DeadlineNotInFutureException.class);

            verifyNoInteractions(publicationRepository);
        }

        @Test
        @DisplayName("should throw DeadlineNotInFutureException when deadline is today")
        void shouldThrowWhenDeadlineIsToday() {
            MembershipFeeTierId tierId = new MembershipFeeTierId(UUID.randomUUID());

            assertThatThrownBy(() -> service.publishYear(new FeeSelectionCampaignManagementPort.PublishYearCommand(
                    YEAR, TODAY, List.of(tierId))))
                    .isInstanceOf(DeadlineNotInFutureException.class);

            verifyNoInteractions(publicationRepository);
        }

        @Test
        @DisplayName("should throw ActiveCampaignExistsException when an active campaign already exists")
        void shouldThrowWhenActiveCampaignExists() {
            LocalDate futureDeadline = TODAY.plusDays(30);
            MembershipFeeTierId tierId = new MembershipFeeTierId(UUID.randomUUID());
            when(publicationRepository.findActive(TODAY)).thenReturn(Optional.of(anActiveCampaign()));

            assertThatThrownBy(() -> service.publishYear(new FeeSelectionCampaignManagementPort.PublishYearCommand(
                    YEAR, futureDeadline, List.of(tierId))))
                    .isInstanceOf(ActiveCampaignExistsException.class);

            verify(publicationRepository, never()).save(any());
        }

        @Test
        @DisplayName("should publish successfully when deadline is in the future and no active campaign exists")
        void shouldPublishWhenValid() {
            LocalDate futureDeadline = TODAY.plusDays(30);
            MembershipFeeTier tier = aTier();
            when(publicationRepository.findActive(TODAY)).thenReturn(Optional.empty());
            when(tierRepository.findById(tier.getId())).thenReturn(Optional.of(tier));
            when(groupRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.publishYear(new FeeSelectionCampaignManagementPort.PublishYearCommand(
                    YEAR, futureDeadline, List.of(tier.getId())));

            verify(publicationRepository).save(any());
        }
    }

    @Nested
    @DisplayName("changeDeadline()")
    class ChangeDeadline {

        private FeeSelectionCampaign campaignWithDeadline(LocalDate deadline) {
            return FeeSelectionCampaign.reconstruct(
                    new FeeSelectionCampaignId(UUID.randomUUID()),
                    YEAR, deadline, null, List.of());
        }

        @Test
        @DisplayName("should load campaign, call changeDeadline on domain object, and save")
        void shouldDelegateToAggregateAndSave() {
            LocalDate newDeadline = TODAY.plusDays(15);
            FeeSelectionCampaign campaign = campaignWithDeadline(TODAY.plusDays(30));
            FeeSelectionCampaignId id = campaign.getId();
            when(publicationRepository.findById(id)).thenReturn(Optional.of(campaign));
            when(publicationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.changeDeadline(id, new FeeSelectionCampaignManagementPort.ChangeDeadlineCommand(newDeadline));

            verify(publicationRepository).save(argThat(c -> c.getVotingDeadline().equals(newDeadline)));
        }

        @Test
        @DisplayName("should throw FeeSelectionCampaignNotFoundException when campaign does not exist")
        void shouldThrowWhenCampaignNotFound() {
            FeeSelectionCampaignId unknownId = new FeeSelectionCampaignId(UUID.randomUUID());
            when(publicationRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.changeDeadline(
                    unknownId, new FeeSelectionCampaignManagementPort.ChangeDeadlineCommand(TODAY.plusDays(10))))
                    .isInstanceOf(FeeSelectionCampaignNotFoundException.class);

            verify(publicationRepository, never()).save(any());
        }
    }
}
