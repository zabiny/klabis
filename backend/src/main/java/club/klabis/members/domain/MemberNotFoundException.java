package club.klabis.members.domain;

import club.klabis.members.MemberId;

public class MemberNotFoundException extends RuntimeException {
    public MemberNotFoundException(String message) {
        super(message);
    }

    public MemberNotFoundException(MemberId memberId) {
        this("Member with ID '%s' doesnt exist".formatted(memberId.value()));
    }
}
