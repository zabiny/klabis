package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.finance.domain.Money;
import com.klabis.members.MemberId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

// public: the generated cross-mapper KlabisConversionServiceAdapter (common.mapping) references
// this interface and its nested MemberBalance record from outside this package.
@Mapper(config = MapstructSpringMapperConfig.class)
public interface MemberAccountResourceConverter extends MoneyMapping, Converter<MemberAccountResourceConverter.MemberBalance, MemberAccountResource> {

    record MemberBalance(MemberId memberId, Money balance) {
    }

    @Override
    @Mapping(target = "memberId", source = "memberId.value")
    @Mapping(target = "currency", source = "balance", qualifiedByName = "currencyCode")
    MemberAccountResource convert(MemberBalance source);
}
