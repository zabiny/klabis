package com.klabis.members;

import com.klabis.common.users.UserId;

import java.time.LocalDateTime;
import java.util.UUID;

public record MemberDto(UUID memberId, String firstName, String lastName, String email, LocalDateTime lastModifiedAt) {

    public MemberDto(UUID memberId, String firstName, String lastName, String email) {
        this(memberId, firstName, lastName, email, LocalDateTime.now());
    }

    public UserId getUserId() {
        return new UserId(memberId);
    }
}
