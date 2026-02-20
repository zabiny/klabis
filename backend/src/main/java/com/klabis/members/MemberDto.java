package com.klabis.members;

import java.time.LocalDateTime;

public record MemberDto(String firstName, String lastName, String email, LocalDateTime lastModifiedAt) {

    public MemberDto(String firstName, String lastName, String email) {
        this(firstName, lastName, email, LocalDateTime.now());
    }

}
