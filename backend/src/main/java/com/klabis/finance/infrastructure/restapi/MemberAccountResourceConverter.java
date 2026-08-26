package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
public interface MemberAccountResourceConverter extends Converter<MemberAccountResourceConverter.MemberBalance, MemberAccountResource> {

    record MemberBalance(MemberId memberId, Money balance) {
    }

    @Override
    @Mapping(target = "memberId", expression = "java(source.memberId().uuid())")
    @Mapping(target = "balance", expression = "java(source.balance().amount())")
    @Mapping(target = "currency", expression = "java(source.balance().currency().getCurrencyCode())")
    MemberAccountResource convert(MemberBalance source);
}
