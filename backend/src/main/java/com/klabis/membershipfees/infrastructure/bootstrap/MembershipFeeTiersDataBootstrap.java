package com.klabis.membershipfees.infrastructure.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.events.application.EventTypeManagementPort;
import com.klabis.events.domain.EventType;
import com.klabis.events.infrastructure.bootstrap.EventTypeDataBootstrap;
import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@Order(5)
public class MembershipFeeTiersDataBootstrap implements BootstrapDataInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(MembershipFeeTiersDataBootstrap.class);

    private final MembershipFeeTierManagementPort tierManagement;
    private final EventTypeManagementPort eventTypeManagement;

    MembershipFeeTiersDataBootstrap(MembershipFeeTierManagementPort tierManagement,
                                    EventTypeManagementPort eventTypeManagement) {
        this.tierManagement = tierManagement;
        this.eventTypeManagement = eventTypeManagement;
    }

    @Override
    public boolean requiresBootstrap() {
        return tierManagement.listTiers().isEmpty();
    }

    @Override
    public void bootstrapData() {
        List<EventType> allTypes = eventTypeManagement.listAllSorted();
        EventTypeReference raceType = findEventTypeByName(allTypes, EventTypeDataBootstrap.RACE_TYPE_NAME);
        EventTypeReference trainingType = findEventTypeByName(allTypes, EventTypeDataBootstrap.TRAINING_TYPE_NAME);

        if (raceType == null || trainingType == null) {
            LOG.warn("Bootstrap event types not found — skipping membership fee tiers bootstrap");
            return;
        }

        MembershipFeeTierId zakladId = tierManagement.createTier(
                new MembershipFeeTierManagementPort.CreateTierCommand("Základ", Money.ofCzk(BigDecimal.valueOf(1000))));
        tierManagement.addRule(zakladId, new MembershipFeeTierManagementPort.AddRuleCommand(
                MembershipPaymentRule.fixedAmount(trainingType, "Others", Money.ofCzk(BigDecimal.valueOf(100)))));

        MembershipFeeTierId jenOblzId = tierManagement.createTier(
                new MembershipFeeTierManagementPort.CreateTierCommand("Jen Oblz", Money.ofCzk(BigDecimal.valueOf(3500))));
        tierManagement.addRule(jenOblzId, new MembershipFeeTierManagementPort.AddRuleCommand(
                MembershipPaymentRule.percentage(raceType, "Oblastni zebricek", 0)));

        MembershipFeeTierId allInclusiveId = tierManagement.createTier(
                new MembershipFeeTierManagementPort.CreateTierCommand("All inclusive", Money.ofCzk(BigDecimal.valueOf(5500))));
        tierManagement.addRule(allInclusiveId, new MembershipFeeTierManagementPort.AddRuleCommand(
                MembershipPaymentRule.percentage(raceType, "Oblastni zebricek", 0)));
        tierManagement.addRule(allInclusiveId, new MembershipFeeTierManagementPort.AddRuleCommand(
                MembershipPaymentRule.percentage(raceType, "Zebricek B", 0)));
        tierManagement.addRule(allInclusiveId, new MembershipFeeTierManagementPort.AddRuleCommand(
                MembershipPaymentRule.percentage(raceType, "Zebricek A", 0)));

        LOG.info("Created 3 bootstrap membership fee tiers (Základ, Jen Oblz, All inclusive)");
    }

    private static EventTypeReference findEventTypeByName(List<EventType> types, String name) {
        return types.stream()
                .filter(t -> name.equalsIgnoreCase(t.getName()))
                .findFirst()
                .map(t -> EventTypeReference.of(t.getId().value()))
                .orElse(null);
    }
}
