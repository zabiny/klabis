package com.klabis.events.infrastructure.restapi;

import com.klabis.events.domain.Money;

interface EntryFeeDtoMapping {

    default EntryFeeDto toEntryFeeDto(Money fee) {
        if (fee == null) {
            return null;
        }
        return EntryFeeDtoBuilder.builder()
                .amount(fee.amount())
                .currency(fee.currency().getCurrencyCode())
                .build();
    }
}
