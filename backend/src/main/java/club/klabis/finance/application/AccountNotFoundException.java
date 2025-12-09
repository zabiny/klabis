package club.klabis.finance.application;

import club.klabis.members.MemberId;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(MemberId memberId) {
        super("Account with id %s not found".formatted(memberId));
    }
}
