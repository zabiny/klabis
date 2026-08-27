package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface MembershipFeeTierResponseConverter extends Converter<MembershipFeeTier, MembershipFeeTierResponse> {

    @Override
    @Mapping(target = "id", expression = "java(level.getId().value())")
    @Mapping(target = "yearlyFeeAmount", expression = "java(level.getYearlyFee().amount())")
    @Mapping(target = "yearlyFeeCurrency", expression = "java(level.getYearlyFee().currency().getCurrencyCode())")
    MembershipFeeTierResponse convert(MembershipFeeTier level);

    @Mapping(target = "id", expression = "java(level.getId().value())")
    @Mapping(target = "yearlyFeeAmount", expression = "java(level.getYearlyFee().amount())")
    @Mapping(target = "yearlyFeeCurrency", expression = "java(level.getYearlyFee().currency().getCurrencyCode())")
    @Mapping(target = "ruleCount", expression = "java(level.getRules().size())")
    MembershipFeeTierSummaryResponse toSummaryResponse(MembershipFeeTier level);
}
