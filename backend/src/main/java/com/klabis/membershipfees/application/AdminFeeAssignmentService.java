package com.klabis.membershipfees.application;

import com.klabis.finance.application.ChargePort;
import com.klabis.membershipfees.MemberFeeSelectionResolvedEvent;
import com.klabis.membershipfees.domain.AssignmentSource;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import com.klabis.membershipfees.domain.FeeSelectionCampaignRepository;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipFeeGroupRepository;
import com.klabis.membershipfees.domain.YearlyFeeChargeMarkerRepository;
import org.jmolecules.ddd.annotation.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
class AdminFeeAssignmentService implements AdminFeeAssignmentPort {

    private final MembershipFeeGroupRepository groupRepository;
    private final FeeSelectionCampaignRepository publicationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ChargePort chargePort;
    private final YearlyFeeChargeMarkerRepository markerRepository;

    AdminFeeAssignmentService(MembershipFeeGroupRepository groupRepository,
                              FeeSelectionCampaignRepository publicationRepository,
                              ApplicationEventPublisher eventPublisher,
                              ChargePort chargePort,
                              YearlyFeeChargeMarkerRepository markerRepository) {
        this.groupRepository = groupRepository;
        this.publicationRepository = publicationRepository;
        this.eventPublisher = eventPublisher;
        this.chargePort = chargePort;
        this.markerRepository = markerRepository;
    }

    @Transactional
    @Override
    public void assignLevel(AssignFeeLevel command) {
        MembershipFeeGroup targetGroup = groupRepository.findById(command.groupId())
                .orElseThrow(() -> new MembershipFeeGroupNotFoundException(command.groupId()));

        FeeSelectionCampaign campaign = publicationRepository.findByYear(command.year())
                .orElseThrow(() -> new FeeSelectionCampaignNotFoundException(command.year()));

        groupRepository.findByMemberAndYear(command.targetMemberId(), command.year())
                .filter(currentGroup -> !currentGroup.getId().equals(command.groupId()))
                .ifPresent(currentGroup -> {
                    currentGroup.removeMember(command.targetMemberId());
                    groupRepository.save(currentGroup);
                });

        targetGroup.addMember(command.targetMemberId(), LocalDate.now(), AssignmentSource.ADMIN_ASSIGNMENT, command.adminId());
        groupRepository.save(targetGroup);

        if (campaign.isClosed(LocalDate.now())) {
            chargeImmediately(command, targetGroup);
        }

        // Always publish — listener's unblockMember() is idempotent (no-op when member is not blocked)
        eventPublisher.publishEvent(new MemberFeeSelectionResolvedEvent(command.targetMemberId(), command.year()));
    }

    private void chargeImmediately(AssignFeeLevel command, MembershipFeeGroup targetGroup) {
        if (markerRepository.existsByMemberIdAndYear(command.targetMemberId(), command.year())) {
            return;
        }
        chargePort.charge(new ChargePort.ChargeCommand(
                command.targetMemberId(),
                targetGroup.getYearlyFeeSnapshot().amount(),
                LocalDate.now(),
                "Roční členský příspěvek " + command.year(),
                CampaignProcessor.SYSTEM_USER_ID));
        markerRepository.markCharged(command.targetMemberId(), command.year());
    }
}

