package com.klabis.members;

import java.math.BigDecimal;

/**
 * Thin projection of monetary value used by members module to avoid a dependency on finance's Money type.
 * Finance module fills this when implementing MemberFinancialStatePort.
 */
public record MonetaryAmount(BigDecimal amount, String currency) {
}
