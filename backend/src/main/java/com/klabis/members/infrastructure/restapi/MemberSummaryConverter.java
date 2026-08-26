package com.klabis.members.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.members.domain.Member;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * Implements Spring's {@link Converter} (rather than a plain {@code @Mapper}) so {@code @WebMvcTest}
 * slices pick it up automatically — {@code WebMvcTypeExcludeFilter} always lets {@link Converter}
 * beans through, regardless of the test's {@code controllers} filter. See {@code MonetaryAmountConverter}
 * for the precedent.
 */
@Mapper(config = MapstructSpringMapperConfig.class)
interface MemberSummaryConverter extends Converter<Member, MemberSummaryResponse> {

    @Override
    @Mapping(target = "id", expression = "java(member.getId().value())")
    @Mapping(target = "registrationNumber", source = "registrationNumber.value")
    @Mapping(target = "email", expression = "java(member.getEmail() != null ? member.getEmail().value() : null)")
    MemberSummaryResponse convert(Member member);
}
