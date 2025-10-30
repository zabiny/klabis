package club.klabis.finance.infrastructure.restapi;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@RecordBuilder
public record TransactionItemResponse(LocalDate date, BigDecimal amount, String note) {
}
