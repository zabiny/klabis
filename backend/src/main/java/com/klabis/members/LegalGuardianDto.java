package com.klabis.members;

import java.time.LocalDateTime;
import java.util.UUID;

public record LegalGuardianDto(UUID userId, String firstName, String lastName, String email,
                               LocalDateTime lastModifiedAt) {
}
