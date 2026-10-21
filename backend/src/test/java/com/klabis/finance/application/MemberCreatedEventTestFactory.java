package com.klabis.finance.application;

import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberId;

import java.time.LocalDate;

class MemberCreatedEventTestFactory {

    static MemberCreatedEvent aMinimalEvent(MemberId memberId) {
        return new MemberCreatedEvent(memberId, LocalDate.of(1990, 1, 1));
    }
}
