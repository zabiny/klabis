package com.klabis.members.management;

import java.util.UUID;

/**
 * Application layer DTO for member summary information.
 * <p>
 * This DTO is used by the application layer to transfer minimal member information
 * from the domain layer to the presentation layer. It contains only the essential
 * fields needed for displaying member lists: firstName, lastName, and registrationNumber.
 * <p>
 * This is separate from presentation layer DTOs (e.g., MemberSummaryResponse) to maintain
 * clean separation between application and presentation concerns.
 *
 * @param id                 unique member identifier (UUID)
 * @param firstName          member's first name
 * @param lastName           member's last name
 * @param registrationNumber member's unique registration number in format XXXYYSS
 */
record MemberSummaryDTO(
        UUID id,
        String firstName,
        String lastName,
        String registrationNumber
) {
}
