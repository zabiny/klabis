package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.membershipfees.domain.MembershipFeeType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface MembershipFeeTypeResponseConverter extends MoneyMapping, Converter<MembershipFeeType, MembershipFeeTypeResponse> {

    @Override
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "yearlyFeeAmount", source = "yearlyFee")
    @Mapping(target = "yearlyFeeCurrency", source = "yearlyFee", qualifiedByName = "currencyCode")
    MembershipFeeTypeResponse convert(MembershipFeeType level);

    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "yearlyFeeAmount", source = "yearlyFee")
    @Mapping(target = "yearlyFeeCurrency", source = "yearlyFee", qualifiedByName = "currencyCode")
    @Mapping(target = "ruleCount", source = ".", qualifiedByName = "ruleCount")
    MembershipFeeTypeSummaryResponse toSummaryResponse(MembershipFeeType level);

    @Named("ruleCount")
    default int ruleCount(MembershipFeeType level) {
        return level.getRules().size();
    }
}
