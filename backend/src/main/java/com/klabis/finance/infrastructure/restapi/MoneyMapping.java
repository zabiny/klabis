package com.klabis.finance.infrastructure.restapi;

import com.klabis.finance.domain.Money;
import org.mapstruct.Named;

import java.math.BigDecimal;

interface MoneyMapping {

    default BigDecimal toAmount(Money money) {
        return money == null ? null : money.amount();
    }

    @Named("currencyCode")
    default String toCurrencyCode(Money money) {
        return money == null ? null : money.currency().getCurrencyCode();
    }
}
