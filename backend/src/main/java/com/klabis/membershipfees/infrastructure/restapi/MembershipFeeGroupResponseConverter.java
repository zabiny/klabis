package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

import java.util.UUID;

/**
 * MapStruct mapper for {@link MembershipFeeGroup} to {@link MembershipFeeGroupResponse}, wired as a
 * Spring {@code Converter} via {@code MapstructSpringMapperConfig}. {@code rulesSnapshot} composes
 * with the hand-written {@link MembershipFeesResponseMapper#toResponse(MembershipPaymentRule)} — a
 * sealed-type switch that MapStruct cannot express — as a nested method on this interface (never
 * {@code uses=} on a Converter, see backend-patterns).
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface MembershipFeeGroupResponseConverter extends MoneyMapping, Converter<MembershipFeeGroup, MembershipFeeGroupResponse> {

    @Override
    @Mapping(target = "yearlyFeeAmount", source = "yearlyFeeSnapshot")
    @Mapping(target = "yearlyFeeCurrency", source = "yearlyFeeSnapshot", qualifiedByName = "currencyCode")
    @Mapping(target = "memberCount", source = ".", qualifiedByName = "memberCount")
    MembershipFeeGroupResponse convert(MembershipFeeGroup group);

    @Named("memberCount")
    default int memberCount(MembershipFeeGroup group) {
        return group.memberCount();
    }

    default UUID toUuid(MembershipFeeGroupId id) {
        return id == null ? null : id.value();
    }

    default UUID toUuid(MembershipFeeTierId id) {
        return id == null ? null : id.value();
    }

    MembershipFeeGroupResponseStatus toStatus(PublishedLevelStatus status);

    default PaymentRuleResponse toRuleResponse(MembershipPaymentRule rule) {
        return MembershipFeesResponseMapper.toResponse(rule);
    }
}
