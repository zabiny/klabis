package com.klabis.members;

import com.klabis.common.users.UserId;

import io.soabase.recordbuilder.core.RecordBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@RecordBuilder
public record MemberDto(UUID memberId, String firstName, String lastName, String email, String registrationNumber, LocalDateTime lastModifiedAt, String chipNumber) {

    public UserId getUserId() {
        return new UserId(memberId);
    }
}
