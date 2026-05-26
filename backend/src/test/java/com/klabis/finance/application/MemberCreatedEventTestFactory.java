package com.klabis.finance.application;

import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberId;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.RegistrationNumber;

import java.time.LocalDate;

class MemberCreatedEventTestFactory {

    static MemberCreatedEvent aMinimalEvent(MemberId memberId) {
        return new MemberCreatedEvent(
                memberId,
                new RegistrationNumber("ZBM0001"),
                "Jan",
                "Novák",
                LocalDate.of(1990, 1, 1),
                "CZ",
                Gender.MALE,
                Address.of("Testovací 1", "Praha", "10000", "CZ"),
                null,
                null,
                null
        );
    }
}
