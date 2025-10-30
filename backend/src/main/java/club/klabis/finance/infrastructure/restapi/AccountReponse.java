package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.domain.MoneyAmount;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record AccountReponse(int accountId, MoneyAmount balance) {

}
