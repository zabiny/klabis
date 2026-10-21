package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.membershipfees.domain.MembershipFeeTier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface MembershipFeeTierResponseConverter extends MoneyMapping, Converter<MembershipFeeTier, MembershipFeeTierResponse> {

    @Override
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "yearlyFeeAmount", source = "yearlyFee")
    @Mapping(target = "yearlyFeeCurrency", source = "yearlyFee", qualifiedByName = "currencyCode")
    MembershipFeeTierResponse convert(MembershipFeeTier level);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "yearlyFeeAmount", source = "yearlyFee")
    @Mapping(target = "yearlyFeeCurrency", source = "yearlyFee", qualifiedByName = "currencyCode")
    @Mapping(target = "ruleCount", source = ".", qualifiedByName = "ruleCount")
    MembershipFeeTierSummaryResponse toSummaryResponse(MembershipFeeTier level);

    @Named("ruleCount")
    default int ruleCount(MembershipFeeTier level) {
        return level.getRules().size();
    }
}
