package com.klabis.finance.application;

import com.klabis.common.users.UserId;
import com.klabis.finance.domain.Transaction;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@PrimaryPort
public interface ChargePort {

    UserId SYSTEM_USER_ID = new UserId(UUID.fromString("00000000-0000-0000-0000-000000000000"));

    record ChargeCommand(MemberId memberId, BigDecimal amount, LocalDate occurredAt,
                         String note, UserId recordedBy) {
        public ChargeCommand {
            Assert.notNull(memberId, "MemberId is required");
            Assert.notNull(amount, "Amount is required");
            Assert.notNull(occurredAt, "OccurredAt is required");
            Assert.notNull(recordedBy, "RecordedBy is required");
        }
    }

    Transaction charge(ChargeCommand command);

    Transaction chargeMembershipFee(MemberId memberId, BigDecimal amount, int year);
}
