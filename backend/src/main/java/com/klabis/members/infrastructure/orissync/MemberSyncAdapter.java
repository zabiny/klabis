package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberNotFoundException;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.domain.Address;
import com.klabis.members.domain.BirthNumber;
import com.klabis.members.domain.EmailAddress;
import com.klabis.members.domain.Gender;
import com.klabis.members.domain.Member;
import com.klabis.members.domain.MemberSyncFromOrisBuilder;
import com.klabis.members.domain.Nationality;
import com.klabis.members.domain.PersonalInformation;
import com.klabis.members.domain.PhoneNumber;
import com.klabis.members.domain.RegistrationNumber;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.ExternalVersionToken;
import com.klabis.sync.domain.SyncCapabilities;
import com.klabis.sync.domain.SyncEntityType;
import com.klabis.sync.domain.SyncProjection;
import com.klabis.sync.domain.SynchronizationAdapter;
import org.jmolecules.architecture.hexagonal.Application;

import java.util.Optional;
import java.util.UUID;

/**
 * The ORIS member {@link SynchronizationAdapter} (design.md D1, D3, D4, D6, D13):
 * pull-only, creating the local side but never writing outward — a Klabis member's
 * membership-fee tier, roles, licences and guardian are Klabis-owned and stay
 * invisible to synchronisation because {@link MemberProjection} omits them by
 * construction (design.md D3), not because this adapter special-cases them.
 * <p>
 * Reaches {@code members} through {@link ManagementPort} (read/write the local side)
 * and {@link RegistrationPort} (create the local side from an ORIS import), and
 * reaches ORIS through {@link OrisClubMembers}, matching {@code DisciplineSyncAdapter}
 * and {@code OrisEventSyncAdapter}'s "load the whole list, find by id" pattern for the
 * external read — {@code oris-client}'s club member listing offers no per-id lookup.
 * <p>
 * Declares {@code containsSensitiveData = true} (design.md D13): a birth number and a
 * name are personal data, unlike the public event/discipline data the two existing
 * adapters synchronise.
 * <p>
 * Classified as {@link Application}, matching the existing adapters: this class holds
 * both a driven role (implementing {@code sync}'s {@link SynchronizationAdapter}) and
 * a driving role (calling {@code members}' primary ports), which jMolecules cannot
 * express with a single hexagonal stereotype.
 */
@OrisIntegrationComponent
@Application
class MemberSyncAdapter implements SynchronizationAdapter {

    /**
     * {@code pullOnlyCreating()}'s six flags with {@code containsSensitiveData} flipped to
     * {@code true} (design.md D13) — no factory overload exists for that combination, so this
     * is spelled out positionally: readsLocal, readsExternal, writesLocal, writesExternal,
     * createsLocal, createsExternal, containsSensitiveData.
     */
    private static final SyncCapabilities CAPABILITIES =
            new SyncCapabilities(true, true, true, false, true, false, true);

    private final ManagementPort managementPort;
    private final RegistrationPort registrationPort;
    private final OrisClubMembers orisClubMembers;

    MemberSyncAdapter(ManagementPort managementPort, RegistrationPort registrationPort,
                       OrisClubMembers orisClubMembers) {
        this.managementPort = managementPort;
        this.registrationPort = registrationPort;
        this.orisClubMembers = orisClubMembers;
    }

    @Override
    public SyncEntityType entityType() {
        return SyncEntityType.MEMBER;
    }

    @Override
    public ExternalSystem system() {
        return ExternalSystem.ORIS;
    }

    @Override
    public SyncCapabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public Class<? extends SyncProjection> projectionType() {
        return MemberProjection.class;
    }

    @Override
    public SyncProjection readLocal(String entityId) {
        Member member = managementPort.getMember(toMemberId(entityId));
        return MemberProjectionMapper.fromMember(member);
    }

    @Override
    public SyncProjection readExternal(String externalId) {
        ClubMember clubMember = orisClubMembers.listClubMembers().values().stream()
                .filter(candidate -> externalId.equals(String.valueOf(candidate.id())))
                .findFirst()
                .orElseThrow(() -> new MemberNotFoundException(externalId));
        return MemberProjectionMapper.fromOrisClubMember(clubMember);
    }

    /**
     * Always empty: {@code oris-client}'s club member listing carries no per-record or
     * per-list version signal, mirroring {@code DisciplineSyncAdapter} and
     * {@code OrisEventSyncAdapter}. The engine falls back to a full read on every pass
     * (design.md D3).
     */
    @Override
    public Optional<ExternalVersionToken> externalVersion(String externalId) {
        return Optional.empty();
    }

    /**
     * No {@code @Transactional} here: the transaction boundary lives one layer down, in
     * {@code ManagementService.syncMemberFromOris}.
     */
    @Override
    public void applyToLocal(String entityId, SyncProjection projection) {
        MemberId memberId = toMemberId(entityId);
        MemberProjection memberProjection = (MemberProjection) projection;

        managementPort.syncMemberFromOris(memberId, buildSyncFromOris(memberProjection));
    }

    /**
     * No {@code @Transactional} here: the transaction boundary lives one layer down, in
     * {@code RegistrationService}'s port implementation.
     */
    @Override
    public String createLocal(SyncProjection projection) {
        MemberProjection memberProjection = (MemberProjection) projection;

        Member imported = registrationPort.importMember(buildImportMember(memberProjection));
        return imported.getId().value().toString();
    }

    @Override
    public void applyToExternal(String externalId, SyncProjection projection) {
        throw new UnsupportedOperationException(
                "The ORIS member adapter declares no outward write capability");
    }

    private static Member.SyncFromOris buildSyncFromOris(MemberProjection projection) {
        return MemberSyncFromOrisBuilder.builder()
                .registrationNumber(new RegistrationNumber(projection.registrationNumber()))
                .firstName(projection.firstName())
                .lastName(projection.lastName())
                .dateOfBirth(projection.dateOfBirth())
                .gender(projection.gender())
                .nationality(projection.nationality() != null ? Nationality.of(projection.nationality()) : null)
                .birthNumber(projection.birthNumber() != null ? BirthNumber.of(projection.birthNumber()) : null)
                .email(projection.email() != null ? EmailAddress.of(projection.email()) : null)
                .phone(projection.phone() != null ? PhoneNumber.of(projection.phone()) : null)
                .address(addressOf(projection))
                .chipNumber(projection.chipNumber())
                .build();
    }

    /**
     * {@code projection.chipNumber()} is not carried here: {@link RegistrationPort.RegisterNewMember}
     * has no chip-number component at all — {@code Member.register} always starts a member with none,
     * ORIS import or not — so there is nothing to map on the create path.
     */
    private static RegistrationPort.ImportMember buildImportMember(MemberProjection projection) {
        RegistrationPort.RegisterNewMember details = new RegistrationPort.RegisterNewMember(
                PersonalInformation.of(
                        projection.firstName(),
                        projection.lastName(),
                        projection.dateOfBirth(),
                        projection.nationality(),
                        projection.gender()),
                addressOf(projection),
                projection.email() != null ? EmailAddress.of(projection.email()) : null,
                projection.phone() != null ? PhoneNumber.of(projection.phone()) : null,
                null,
                projection.birthNumber() != null ? BirthNumber.of(projection.birthNumber()) : null,
                null,
                null
        );
        return new RegistrationPort.ImportMember(details, new RegistrationNumber(projection.registrationNumber()));
    }

    /**
     * {@link MemberProjectionMapper} already normalises a partial ORIS address to fully absent
     * (design.md ADDRESS), so a {@code null} street here means all four components are null —
     * never a partial set that would blow up {@link Address}'s all-or-nothing constructor.
     */
    private static Address addressOf(MemberProjection projection) {
        if (projection.street() == null) {
            return null;
        }
        return new Address(projection.street(), projection.city(), projection.postalCode(), projection.country());
    }

    private static MemberId toMemberId(String entityId) {
        return new MemberId(UUID.fromString(entityId));
    }
}
