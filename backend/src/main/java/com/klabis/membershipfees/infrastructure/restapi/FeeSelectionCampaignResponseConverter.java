package com.klabis.membershipfees.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.membershipfees.domain.FeeSelectionCampaign;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface FeeSelectionCampaignResponseConverter extends Converter<FeeSelectionCampaign, FeeSelectionCampaignResponse> {

    @Override
    @Mapping(target = "id", source = "id.value")
    FeeSelectionCampaignResponse convert(FeeSelectionCampaign source);
}
