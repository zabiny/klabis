package com.klabis.groups.freegroup.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.common.users.Authority;
import com.klabis.groups.freegroup.application.PendingInvitationView;
import com.klabis.groups.infrastructure.restapi.DelegatedAuthority;
import com.klabis.groups.infrastructure.restapi.PendingInvitationResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

import java.util.Set;

@Mapper(config = MapstructSpringMapperConfig.class)
interface PendingInvitationResponseConverter extends Converter<PendingInvitationView, PendingInvitationResponse> {

    @Override
    @Mapping(target = "groupId", source = "groupId.value")
    @Mapping(target = "invitationId", source = "invitation.id.value")
    @Mapping(target = "invitedBy", source = "invitation.invitedBy.value")
    PendingInvitationResponse convert(PendingInvitationView source);

    default Set<DelegatedAuthority> toWire(Set<Authority> domain) {
        return DelegatedAuthorities.toWire(domain);
    }
}
