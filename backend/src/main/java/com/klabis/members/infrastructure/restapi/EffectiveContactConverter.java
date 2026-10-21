package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.members.application.EffectiveContact;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.core.convert.converter.Converter;

/**
 * Implements {@link Converter} so {@code @WebMvcTest} slices pick it up; see {@link MemberSummaryConverter}.
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface EffectiveContactConverter extends Converter<EffectiveContact, DisplayContact> {

    @Override
    DisplayContact convert(EffectiveContact contact);

    @Mapping(target = "fromGuardian", source = ".", qualifiedByName = "fromGuardian")
    DisplayContactValue valueToDto(EffectiveContact.Value value);

    @Named("fromGuardian")
    default boolean fromGuardian(EffectiveContact.Value value) {
        return value.fromGuardian();
    }
}
