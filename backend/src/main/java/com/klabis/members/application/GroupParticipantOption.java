package com.klabis.members.application;

import java.util.UUID;

/**
 * Person in a group; the id is the member id for a member and the user id of a non-member legal guardian.
 */
public record GroupParticipantOption(UUID id, String displayName) {
}
