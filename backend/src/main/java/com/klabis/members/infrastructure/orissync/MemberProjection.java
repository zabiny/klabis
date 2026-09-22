package com.klabis.members.infrastructure.orissync;

import com.klabis.members.domain.Gender;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;

import java.time.LocalDate;

/**
 * The ORIS-owned fields of a {@code Member}, in the shape shared by both the Klabis
 * {@code Member} side and the ORIS {@code ClubMember} side (design.md D2).
 * <p>
 * Everything else {@code Member} holds — licences, guardian, bank account, dietary
 * restrictions and the whole suspension block — is deliberately absent: per
 * {@link SyncProjection}'s own contract, a field a Klabis module owns exclusively is
 * invisible to synchronisation by construction, not by special-casing. Unlike
 * {@code OrisEventProjection}, there is no Klabis-owned value to smuggle through a
 * {@code @JsonIgnore} component — the external reference (ORIS's {@code ClubMember.id})
 * lives entirely in the {@code sync} module's {@code SyncRecord}, never on this
 * projection.
 * <p>
 * Address is flattened into four strings rather than carrying the {@code Address}
 * value object, matching {@code OrisEventProjection}'s reasoning: a projection is a
 * plain data carrier serialised directly by {@code SyncProjectionCodec}, and value
 * objects serialise unpredictably without extra Jackson wiring the codec deliberately
 * does not carry.
 */
public record MemberProjection(
        String registrationNumber,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String nationality,
        String birthNumber,
        String email,
        String phone,
        String street,
        String city,
        String postalCode,
        String country,
        String chipNumber
) implements SyncProjection {

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.MEMBER;
    }
}
