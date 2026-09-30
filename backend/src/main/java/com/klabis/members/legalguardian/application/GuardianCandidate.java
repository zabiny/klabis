package com.klabis.members.legalguardian.application;

import com.klabis.common.users.UserId;

/**
 * Person offered when choosing a legal guardian; the display label tells namesakes apart by registration
 * number (members) or e-mail (non-members).
 */
public record GuardianCandidate(UserId userId, String displayName, GuardianKind kind, String registrationNumber,
                                String email) {
}
