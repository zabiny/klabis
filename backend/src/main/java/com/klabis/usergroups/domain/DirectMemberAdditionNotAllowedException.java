package com.klabis.usergroups.domain;

public class DirectMemberAdditionNotAllowedException extends RuntimeException {

    public DirectMemberAdditionNotAllowedException() {
        super("Direct member addition is not allowed for invitation-based groups. Use the invitation flow instead.");
    }
}
