package com.klabis.finance.application;

import com.klabis.common.users.UserId;
import com.klabis.finance.domain.Transaction;
import com.klabis.finance.domain.TransactionId;
import com.klabis.members.MemberId;
import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.springframework.util.Assert;

import java.time.LocalDate;

@PrimaryPort
public interface ReversePort {

    record ReverseCommand(MemberId memberId, TransactionId transactionId, String note,
                          LocalDate occurredAt, UserId recordedBy) {
        public ReverseCommand {
            Assert.notNull(memberId, "MemberId is required");
            Assert.notNull(transactionId, "TransactionId is required");
            Assert.notNull(occurredAt, "OccurredAt is required");
            Assert.notNull(recordedBy, "RecordedBy is required");
        }
    }

    Transaction reverse(ReverseCommand command);
}
