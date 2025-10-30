package club.klabis.finance.infrastructure.restapi;

import club.klabis.finance.domain.MoneyAmount;
import club.klabis.members.MemberId;
import io.soabase.recordbuilder.core.RecordBuilder;

@RecordBuilder
public record AccountReponse(MemberId ownerId, MoneyAmount balance) {

}
