package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTierManagementPort;
import com.klabis.membershipfees.domain.EventTypeReference;
import com.klabis.membershipfees.domain.MembershipPaymentRule;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

// Request DTOs are generated from the spec (see build.gradle.kts openApiModule("membershipfees")),
// so the request -> domain/command mapping that used to live as instance methods on the
// hand-written DTOs now lives here.
final class MembershipFeesRequestMapper {

    private MembershipFeesRequestMapper() {
    }

    static MembershipFeeTierManagementPort.CreateTierCommand toCommand(CreateMembershipFeeTierRequest request) {
        Money yearlyFee = toMoney(request.yearlyFeeAmount(), request.yearlyFeeCurrency());
        return new MembershipFeeTierManagementPort.CreateTierCommand(request.name(), yearlyFee);
    }

    static MembershipFeeTierManagementPort.EditTierCommand toCommand(EditMembershipFeeTierRequest request) {
        Money yearlyFee = request.yearlyFeeAmount() != null
                ? toMoney(request.yearlyFeeAmount(), request.yearlyFeeCurrency())
                : null;
        List<MembershipPaymentRule> domainRules = request.rules() == null ? null
                : request.rules().stream().map(MembershipFeesRequestMapper::toDomain).toList();
        return new MembershipFeeTierManagementPort.EditTierCommand(request.name(), yearlyFee, domainRules);
    }

    static MembershipPaymentRule toDomain(AddPaymentRuleRequest request) {
        return toRule(request.eventTypeId(), request.rankingShortName(), request.ruleType(),
                request.percentage(), request.fixedAmount(), request.fixedCurrency());
    }

    static MembershipPaymentRule.RuleValue toRuleValue(EditPaymentRuleRequest request, UUID eventTypeId,
                                                        String rankingShortName) {
        return toRule(eventTypeId, rankingShortName, request.ruleType(),
                request.percentage(), request.fixedAmount(), request.fixedCurrency()).value();
    }

    static MembershipPaymentRule toDomain(PaymentRuleRequest request) {
        return toRule(request.eventTypeId(), request.rankingShortName(), request.ruleType(),
                request.percent(), request.fixedAmount(), request.fixedCurrency());
    }

    static FeeSelectionCampaignManagementPort.PublishYearCommand toCommand(PublishYearRequest request) {
        return new FeeSelectionCampaignManagementPort.PublishYearCommand(request.year(), request.votingDeadline(),
                request.levelIds().stream().map(MembershipFeeTierId::new).toList());
    }

    static FeeSelectionCampaignManagementPort.ChangeDeadlineCommand toCommand(ChangeDeadlineRequest request) {
        return new FeeSelectionCampaignManagementPort.ChangeDeadlineCommand(request.votingDeadline());
    }

    static FeeSelectionCampaignManagementPort.EditGroupSnapshotCommand toCommand(EditGroupSnapshotRequest request) {
        Money yearlyFee = toMoney(request.yearlyFeeAmount(), request.yearlyFeeCurrency());
        List<MembershipPaymentRule> domainRules = request.rules() == null ? List.of()
                : request.rules().stream().map(MembershipFeesRequestMapper::toDomain).toList();
        return new FeeSelectionCampaignManagementPort.EditGroupSnapshotCommand(yearlyFee, domainRules);
    }

    private static MembershipPaymentRule toRule(UUID eventTypeId, String rankingShortName, String ruleType,
                                                 Integer percentage, BigDecimal fixedAmount, String fixedCurrency) {
        EventTypeReference evtTypeId = EventTypeReference.of(eventTypeId);
        return switch (ruleType) {
            case "PERCENTAGE" -> MembershipPaymentRule.percentage(evtTypeId, rankingShortName, percentage);
            case "FIXED_AMOUNT" -> MembershipPaymentRule.fixedAmount(evtTypeId, rankingShortName,
                    toMoney(fixedAmount, fixedCurrency));
            default -> throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        };
    }

    private static Money toMoney(BigDecimal amount, String currencyCode) {
        String currency = currencyCode != null ? currencyCode : "CZK";
        return Money.of(amount, Currency.getInstance(currency));
    }
}
