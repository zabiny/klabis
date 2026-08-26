package com.klabis.finance.infrastructure.restapi;

import com.klabis.common.mapping.MapstructSpringMapperConfig;
import com.klabis.finance.domain.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(config = MapstructSpringMapperConfig.class)
interface TransactionResourceConverter extends Converter<Transaction, TransactionResource> {

    @Override
    @Mapping(target = "id", expression = "java(tx.getId().value())")
    @Mapping(target = "type", expression = "java(tx.getType().name())")
    @Mapping(target = "amount", expression = "java(tx.getAmount().amount())")
    @Mapping(target = "currency", expression = "java(tx.getAmount().currency().getCurrencyCode())")
    @Mapping(target = "recordedBy", expression = "java(tx.getRecordedBy().uuid())")
    @Mapping(target = "reversesTransactionId",
            expression = "java(tx.getReversesTransactionId() != null ? tx.getReversesTransactionId().value() : null)")
    TransactionResource convert(Transaction tx);
}
