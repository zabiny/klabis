package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.finance.domain.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface TransactionResourceConverter extends MoneyMapping, Converter<Transaction, TransactionResource> {

    @Override
    @Mapping(target = "id", source = "id.value")
    @Mapping(target = "currency", source = "amount", qualifiedByName = "currencyCode")
    @Mapping(target = "recordedBy", source = "recordedBy.uuid")
    @Mapping(target = "reversesTransactionId", source = "reversesTransactionId.value")
    TransactionResource convert(Transaction source);
}
