package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.membershipfees.MembershipFeeGroupId;
import com.klabis.membershipfees.MembershipFeeTierId;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import com.klabis.membershipfees.domain.MembershipPaymentRule;
import com.klabis.membershipfees.domain.PublishedLevelStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
interface MembershipFeeGroupResponseConverter extends Converter<MembershipFeeGroup, MembershipFeeGroupResponse> {

    @Override
    @Mapping(target = "id", source = "id")
    @Mapping(target = "sourceLevelId", source = "sourceLevelId")
    @Mapping(target = "yearlyFeeAmount", expression = "java(group.getYearlyFeeSnapshot().amount())")
    @Mapping(target = "yearlyFeeCurrency", expression = "java(group.getYearlyFeeSnapshot().currency().getCurrencyCode())")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "memberCount", expression = "java(group.memberCount())")
    @Mapping(target = "rulesSnapshot", source = "rulesSnapshot")
    MembershipFeeGroupResponse convert(MembershipFeeGroup group);

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
