package com.klabis.common.users.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.springframework.core.convert.converter.Converter;

/**
 * Implements Spring's {@link Converter} (rather than a plain {@code @Mapper}) so {@code @WebMvcTest}
 * slices pick it up automatically — {@code WebMvcTypeExcludeFilter} always lets {@link Converter}
 * beans through, regardless of the test's {@code controllers} filter. See {@code MonetaryAmountConverter}
 * for the precedent.
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface AuthorityToDtoConverter extends Converter<com.klabis.common.users.Authority, Authority> {

    /**
     * {@code DEVELOPER} is an internal-only authority, deliberately absent from the public
     * {@code Authority} wire schema and the permissions dialog. It must never reach a DTO;
     * hitting this mapping means it leaked past {@code UserPermissions.getManageableAuthorities()}.
     * The coordinator, group ownership, account viewing and profile viewing authorities are held over specific targets only,
     * never directly, so they cannot appear in a user's permissions either.
     */
    @Override
    @ValueMapping(source = "DEVELOPER", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "EVENTS_EDIT", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "EVENTS_REGISTRATIONS_OVERVIEW", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "MEMBERS_VIEW_PROFILE", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "GROUPS_OWNER", target = MappingConstants.THROW_EXCEPTION)
    @ValueMapping(source = "FINANCE_VIEW", target = MappingConstants.THROW_EXCEPTION)
    Authority convert(com.klabis.common.users.Authority source);
}
