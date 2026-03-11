package com.klabis.members.infrastructure.restapi;

import com.klabis.common.patch.PatchField;
import com.klabis.common.validation.ValidPatchFieldSize;

/**
 * Request DTO for self-service update of own member profile.
 * <p>
 * Members can only update a subset of their own information.
 * Admin-only fields (firstName, lastName, etc.) are not included.
 */
public record SelfUpdateMemberRequest(
        PatchField<String> email,

        PatchField<String> phone,

        PatchField<AddressRequest> address,

        @ValidPatchFieldSize(max = 500, message = "Dietary restrictions must not exceed 500 characters")
        PatchField<String> dietaryRestrictions
) {
}
