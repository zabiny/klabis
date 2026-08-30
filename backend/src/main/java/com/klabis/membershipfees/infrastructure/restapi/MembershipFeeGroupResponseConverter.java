package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.membershipfees.domain.MembershipFeeGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface MembershipFeeGroupResponseConverter extends Converter<MembershipFeeGroup, MembershipFeeGroupResponse> {

    @Override
    @Mapping(target = "id", expression = "java(group.getId().value())")
    @Mapping(target = "sourceLevelId", expression = "java(group.getSourceLevelId().value())")
    @Mapping(target = "yearlyFeeAmount", expression = "java(group.getYearlyFeeSnapshot().amount())")
    @Mapping(target = "yearlyFeeCurrency", expression = "java(group.getYearlyFeeSnapshot().currency().getCurrencyCode())")
    @Mapping(target = "status", expression = "java(MembershipFeeGroupResponseStatus.valueOf(group.getStatus().name()))")
    @Mapping(target = "memberCount", expression = "java(group.memberCount())")
    @Mapping(target = "rulesSnapshot", expression = "java(group.getRulesSnapshot().stream().map(MembershipFeesResponseMapper::toResponse).toList())")
    MembershipFeeGroupResponse convert(MembershipFeeGroup group);
}
