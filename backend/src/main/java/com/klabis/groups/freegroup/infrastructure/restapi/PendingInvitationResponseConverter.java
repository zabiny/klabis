package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.groups.freegroup.application.PendingInvitationView;
import com.klabis.groups.infrastructure.restapi.PendingInvitationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface PendingInvitationResponseConverter extends Converter<PendingInvitationView, PendingInvitationResponse> {

    @Override
    @Mapping(target = "groupId", expression = "java(view.groupId().uuid())")
    @Mapping(target = "invitationId", expression = "java(view.invitation().getId().value())")
    @Mapping(target = "invitedBy", expression = "java(view.invitation().getInvitedBy().uuid())")
    PendingInvitationResponse convert(PendingInvitationView view);
}
