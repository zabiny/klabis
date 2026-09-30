package com.klabis.members.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.common.users.UserId;
import com.klabis.members.BirthNumberAccessedEvent;
import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberId;
import com.klabis.members.MemberResumedEvent;
import com.klabis.members.MemberSuspendedEvent;
import io.soabase.recordbuilder.core.RecordBuilder;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.util.Assert;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Member aggregate root.
 * <p>
 * Represents a club member with personal information, contacts, and registration details.
 * This is the aggregate root for the Members bounded context.
 * <p>
 * Business invariants:
 * - Registration number must be unique
 * - Rodne cislo only allowed for Czech nationality
 * - Completeness of contacts and legal guardians is derived by {@link MemberCompleteness}
 */
@AggregateRoot
public class Member extends KlabisAggregateRoot<Member, MemberId> {

    @Identity
    private final MemberId id;
    private final RegistrationNumber registrationNumber;

    // Value objects
    private PersonalInformation personalInformation;
    private Address address;
    private EmailAddress email;
    private PhoneNumber phone;
    private boolean active;
    private String chipNumber;
    private IdentityCard identityCard;
    private MedicalCourse medicalCourse;
    private TrainerLicense trainerLicense;
    private RefereeLicense refereeLicense;
    private DrivingLicenseGroup drivingLicenseGroup;
    private String dietaryRestrictions;
    private BirthNumber birthNumber;
    private BankAccountNumber bankAccountNumber;
    private boolean dataIncomplete;

    // Suspension fields
    private DeactivationReason suspensionReason;
    private Instant suspendedAt;
    private String suspensionNote;
    private UserId suspendedBy;

    // ========== Command Records ==========

    /**
     * Command to register a new member with a specific ID.
     * <p>
     * This command is used when the Member ID needs to be shared with another aggregate
     * (e.g., User aggregate) to ensure both aggregates use the same identifier.
     */
    @RecordBuilder
    public record RegisterMember(
            MemberId id,
            RegistrationNumber registrationNumber,
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber,
            UserId registeredBy
    ) {
        public static RegisterMember from(Member member) {
            return new RegisterMember(
                    member.id,
                    member.registrationNumber,
                    member.personalInformation,
                    member.address,
                    member.email,
                    member.phone,
                    member.birthNumber,
                    member.bankAccountNumber,
                    null
            );
        }
    }

    /**
     * Command for updating a member's profile.
     * <p>
     * Covers all updatable fields. Authorization at the API layer determines which fields
     * a given caller is permitted to set — admin-only fields (firstName, lastName, dateOfBirth,
     * gender, birthNumber) are blocked for non-admins before the command reaches the domain.
     * <p>
     * Every field carries a concrete value: the command is always a full snapshot of the intended
     * end state, so {@link #update(UpdateMember)} applies each field unconditionally. Callers build
     * it by taking {@link #from(Member)} as the baseline and overlaying only the fields the request
     * actually changed — a field left at its baseline value is applied as a no-op. The PATCH
     * "undefined vs. present-null" distinction lives entirely in the REST mapper, not here.
     */
    @RecordBuilder
    public record UpdateMember(
            EmailAddress email,
            PhoneNumber phone,
            Address address,
            String chipNumber,
            String nationality,
            BankAccountNumber bankAccountNumber,
            IdentityCard identityCard,
            DrivingLicenseGroup drivingLicenseGroup,
            MedicalCourse medicalCourse,
            TrainerLicense trainerLicense,
            RefereeLicense refereeLicense,
            String dietaryRestrictions,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Gender gender,
            BirthNumber birthNumber,
            UserId updatedBy
    ) {

        /**
         * The baseline command: every field carries the member's current value, so applying it
         * as-is is a no-op. REST callers overlay only the fields their PATCH request changed.
         */
        public static UpdateMember from(Member member) {
            PersonalInformation pi = member.personalInformation;
            return new UpdateMember(
                    member.email,
                    member.phone,
                    member.address,
                    member.chipNumber,
                    pi != null ? pi.getNationalityCode() : null,
                    member.bankAccountNumber,
                    member.identityCard,
                    member.drivingLicenseGroup,
                    member.medicalCourse,
                    member.trainerLicense,
                    member.refereeLicense,
                    member.dietaryRestrictions,
                    pi != null ? pi.getFirstName() : null,
                    pi != null ? pi.getLastName() : null,
                    pi != null ? pi.getDateOfBirth() : null,
                    pi != null ? pi.getGender() : null,
                    member.birthNumber,
                    null
            );
        }
    }

    /**
     * Command to suspend a member's membership.
     * <p>
     * This command is used by administrators to suspend a member's membership
     * with a specific reason and optional note.
     */
    @RecordBuilder
    public record SuspendMembership(
            UserId suspendedBy,
            DeactivationReason reason,
            String note
    ) {
        public static SuspendMembership from(Member member) {
            return new SuspendMembership(member.suspendedBy, member.suspensionReason, member.suspensionNote);
        }
    }

    /**
     * Command to resume a suspended member's membership.
     * <p>
     * This command is used by administrators to resume a member's membership
     * that was previously suspended.
     */
    @RecordBuilder
    public record ResumeMembership(
            UserId resumedBy
    ) {
        public static ResumeMembership from(Member member) {
            return new ResumeMembership(null);
        }
    }

    /**
     * Command carrying an inward write from ORIS synchronisation.
     * <p>
     * Carries exactly the fields ORIS owns — see design.md D2/D3. Licences, bank account,
     * dietary requirements and the suspension block are not expressible here, so a synchronisation
     * cannot touch them even by mistake. Each field is written unconditionally by
     * {@link #syncFromOris(SyncFromOris)}, including when it is {@code null}: protecting a
     * Klabis-entered value against an empty ORIS field is the synchronisation engine's job, decided
     * before this method is ever reached, not this method's.
     * <p>
     * {@code registrationNumber} is not applied — {@link Member#registrationNumber} is set only at
     * construction — but is checked against the target aggregate as a guard against the caller
     * having resolved the wrong member. {@code nationality} is a validated {@link Nationality}
     * rather than the raw code {@link UpdateMember} still takes, since the ORIS projection this
     * command is built from has already produced one.
     */
    @RecordBuilder
    public record SyncFromOris(
            RegistrationNumber registrationNumber,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Gender gender,
            Nationality nationality,
            BirthNumber birthNumber,
            EmailAddress email,
            PhoneNumber phone,
            Address address,
            String chipNumber
    ) {
    }

    // ========== Constructors ==========

    private Member(
            MemberId id,
            RegistrationNumber registrationNumber,
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            boolean active,
            String chipNumber,
            IdentityCard identityCard,
            MedicalCourse medicalCourse,
            TrainerLicense trainerLicense,
            RefereeLicense refereeLicense,
            DrivingLicenseGroup drivingLicenseGroup,
            String dietaryRestrictions,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber,
            boolean dataIncomplete,
            DeactivationReason suspensionReason,
            Instant suspendedAt,
            String suspensionNote,
            UserId suspendedBy) {

        this.id = id;
        this.registrationNumber = registrationNumber;
        this.personalInformation = personalInformation;
        this.address = address;
        this.email = email;
        this.phone = phone;
        this.active = active;
        this.chipNumber = chipNumber;
        this.identityCard = identityCard;
        this.medicalCourse = medicalCourse;
        this.trainerLicense = trainerLicense;
        this.refereeLicense = refereeLicense;
        this.drivingLicenseGroup = drivingLicenseGroup;
        this.dietaryRestrictions = dietaryRestrictions;
        this.birthNumber = birthNumber;
        this.bankAccountNumber = bankAccountNumber;
        this.dataIncomplete = dataIncomplete;
        this.suspensionReason = suspensionReason;
        this.suspendedAt = suspendedAt;
        this.suspensionNote = suspensionNote;
        this.suspendedBy = suspendedBy;
    }

    /**
     * Factory method for reconstructing Member from persistence layer.
     * This bypasses validation since the data was already validated when originally stored.
     * <p>
     * This method is public only for infrastructure/persistence layer usage.
     * Use {@link #register(RegisterMember)} for creating new members.
     * <p>
     * <b>IMPORTANT:</b> This method is used by MemberMemento.toMember() to reconstruct
     * members after loading from the database.
     *
     * @param id                  member's unique identifier
     * @param registrationNumber  member's registration number
     * @param personalInformation member's personal information
     * @param address             member's address
     * @param email               member's email address
     * @param phone               member's phone number
     * @param active              whether the member is active
     * @param chipNumber          member's chip number (may be null)
     * @param identityCard        member's identity card (may be null)
     * @param medicalCourse       member's medical course (may be null)
     * @param trainerLicense      member's trainer license (may be null)
     * @param drivingLicenseGroup member's driving license group (may be null)
     * @param dietaryRestrictions member's dietary restrictions (may be null)
     * @param birthNumber         member's birth number (may be null)
     * @param bankAccountNumber   member's bank account number (may be null)
     * @param dataIncomplete      materialised completeness flag as of the member's last save
     * @param suspensionReason    reason for suspension (may be null)
     * @param suspendedAt         timestamp of suspension (may be null)
     * @param suspensionNote      optional suspension note (may be null)
     * @param suspendedBy         user who suspended (may be null)
     * @return reconstructed Member instance
     */
    public static Member reconstruct(
            MemberId id,
            RegistrationNumber registrationNumber,
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            boolean active,
            String chipNumber,
            IdentityCard identityCard,
            MedicalCourse medicalCourse,
            TrainerLicense trainerLicense,
            RefereeLicense refereeLicense,
            DrivingLicenseGroup drivingLicenseGroup,
            String dietaryRestrictions,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber,
            boolean dataIncomplete,
            DeactivationReason suspensionReason,
            Instant suspendedAt,
            String suspensionNote,
            UserId suspendedBy,
            AuditMetadata auditMetadata) {

        Member member = new Member(
                id,
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                active,
                chipNumber,
                identityCard,
                medicalCourse,
                trainerLicense,
                refereeLicense,
                drivingLicenseGroup,
                dietaryRestrictions,
                birthNumber,
                bankAccountNumber,
                dataIncomplete,
                suspensionReason,
                suspendedAt,
                suspensionNote,
                suspendedBy
        );
        member.updateAuditMetadata(auditMetadata);
        // No domain events for reconstructed entities
        return member;
    }

    /**
     * Registers a hand-filled member, which must be complete (design.md D5). Contacts and legal guardians of
     * a minor are not known yet at this point - they are set on the legal guardian group afterwards - so for
     * a minor the e-mail, telephone and guardian requirements are the caller's to enforce, while an adult's
     * own contacts are enforced here.
     */
    public static Member register(RegisterMember command) {
        validateStructure(command);

        // Consistency rule: always enforced, regardless of completeness
        validateBirthNumberConsistency(command.personalInformation().getNationalityCode(), command.birthNumber());

        Set<MissingDataItem> missing = MemberCompleteness.missingData(command.email(), command.phone(),
                command.personalInformation(), command.birthNumber(), command.address(), GuardianContacts.NONE);
        if (command.personalInformation().isMinor()) {
            missing.removeAll(Set.of(MissingDataItem.EMAIL, MissingDataItem.PHONE, MissingDataItem.GUARDIAN));
        }
        enforceCompleteness(missing);

        return buildFrom(command);
    }

    /**
     * Registers a member whose data comes from ORIS rather than a hand-filled form (design.md
     * D5), used only by {@code RegistrationPort.importMember}. Unlike {@link #register}, no
     * completeness rule is enforced here: ORIS never holds a legal guardian and often lacks contact
     * details, and the goal is to bring every current club member in regardless. The consistency
     * rule — a birth number is never accepted for a non-Czech national — still holds, since it is
     * not about completeness but about a value that would otherwise be simply wrong.
     */
    public static Member importFromOris(RegisterMember command) {
        validateStructure(command);

        // Consistency rule: always enforced, even when completeness is not (design.md D5)
        validateBirthNumberConsistency(command.personalInformation().getNationalityCode(), command.birthNumber());

        return buildFrom(command);
    }

    private static void validateStructure(RegisterMember command) {
        Assert.notNull(command.id(), "Member ID is required");
        Assert.notNull(command.registrationNumber(), "Registration number is required");
        Assert.notNull(command.personalInformation(), "Personal information is required");
        // Address is no longer required here: an ORIS import may bring a member in without one
        // (design.md D5/ADDRESS), and register() enforces it separately through
        // enforceCompleteness so the exception type/message a hand registration sees is unchanged.
    }

    private static Member buildFrom(RegisterMember command) {
        Member member = new Member(
                command.id(),
                command.registrationNumber(),
                command.personalInformation(),
                command.address(),
                command.email(),
                command.phone(),
                true, // new members are active by default
                null, // chipNumber
                null, // identityCard
                null, // medicalCourse
                null, // trainerLicense
                null, // refereeLicense
                null, // drivingLicenseGroup
                null, // dietaryRestrictions
                command.birthNumber(),
                command.bankAccountNumber(),
                !MemberCompleteness.missingData(command.email(), command.phone(), command.personalInformation(),
                        command.birthNumber(), command.address(), GuardianContacts.NONE).isEmpty(),
                null, // suspensionReason
                null, // suspendedAt
                null, // suspensionNote
                null  // suspendedBy
        );

        // Register domain event
        member.registerEvent(MemberCreatedEvent.fromAggregate(member));

        if (command.birthNumber() != null && command.registeredBy() != null) {
            member.registerEvent(BirthNumberAccessedEvent.modified(command.registeredBy(), member.getId()));
        }

        return member;
    }

    /**
     * Validates birth number/nationality consistency: a birth number is forbidden for non-Czech
     * nationals. This rule always holds, independent of completeness (design.md D3/D5) — unlike
     * the requirement that a Czech national *have* a birth number, which is a completeness rule
     * (see {@link MissingDataItem#BIRTH_NUMBER}).
     *
     * @param nationalityCode the member's nationality code (ISO 3166-1)
     * @param birthNumber     the birth number to validate (may be null)
     * @throws BusinessRuleViolationException if a birth number is provided for a non-Czech national
     */
    private static void validateBirthNumberConsistency(String nationalityCode, BirthNumber birthNumber) {
        Nationality nationality = Nationality.of(nationalityCode);

        if (birthNumber != null && !nationality.isCzech()) {
            throw new BusinessRuleViolationException(
                    "Birth number is only allowed for Czech nationals"
            ) {
            };
        }
    }

    /**
     * Stores whether the member is incomplete, so the member list and its filter can read it without loading
     * legal guardians. The value is only as fresh as the member's last save.
     */
    public void recordMissingData(Set<MissingDataItem> missing) {
        this.dataIncomplete = !missing.isEmpty();
    }

    public boolean isDataIncomplete() {
        return dataIncomplete;
    }

    /**
     * Enforces full completeness (design.md D5) — used by {@link #register(RegisterMember)}, where
     * every missing item is a violation. Exception types and messages match the pre-completeness
     * validation methods this replaced, so the registration form's behaviour is unchanged.
     */
    private static void enforceCompleteness(Set<MissingDataItem> missing) {
        Assert.isTrue(!missing.contains(MissingDataItem.EMAIL),
                "At least one email address is required (member or guardian)");
        Assert.isTrue(!missing.contains(MissingDataItem.PHONE),
                "At least one phone number is required (member or guardian)");
        Assert.isTrue(!missing.contains(MissingDataItem.ADDRESS), "Address is required");

        if (missing.contains(MissingDataItem.GUARDIAN)) {
            throw new BusinessRuleViolationException(
                    "Guardian is required for minors (under 18 years)"
            ) {
            };
        }
        if (missing.contains(MissingDataItem.BIRTH_NUMBER)) {
            throw new BusinessRuleViolationException(
                    "Birth number is required for Czech nationals"
            ) {
            };
        }
    }

    /**
     * Enforces the never-worsen rule (design.md D5) — used by {@link #update(UpdateMember)}. Only
     * an item that becomes missing as a result of the edit is a violation; an item that was already
     * missing before the edit may remain missing (an incomplete member may be saved with unrelated
     * changes, or with only some missing items filled in).
     */
    private static void enforceNeverWorsen(Set<MissingDataItem> before, Set<MissingDataItem> after,
                                           boolean becameMinor) {
        if (after.contains(MissingDataItem.EMAIL) && !before.contains(MissingDataItem.EMAIL)) {
            throw new IllegalArgumentException("At least one email address is required (member or guardian)");
        }
        if (after.contains(MissingDataItem.PHONE) && !before.contains(MissingDataItem.PHONE)) {
            throw new IllegalArgumentException("At least one phone number is required (member or guardian)");
        }
        if (after.contains(MissingDataItem.ADDRESS) && !before.contains(MissingDataItem.ADDRESS)) {
            throw new IllegalArgumentException("Address is required");
        }
        if (!becameMinor && after.contains(MissingDataItem.GUARDIAN) && !before.contains(MissingDataItem.GUARDIAN)) {
            throw new BusinessRuleViolationException(
                    "Guardian is required for minors (under 18 years)"
            ) {
            };
        }
        if (after.contains(MissingDataItem.BIRTH_NUMBER) && !before.contains(MissingDataItem.BIRTH_NUMBER)) {
            throw new BusinessRuleViolationException(
                    "Birth number is required for Czech nationals"
            ) {
            };
        }
    }

    // ========== Getters ==========

    @Override
    public MemberId getId() {
        return this.id;
    }

    public UserId getUserId() {
        return this.id.toUserId();
    }

    public RegistrationNumber getRegistrationNumber() {
        return this.registrationNumber;
    }

    public PersonalInformation getPersonalInformation() {
        return personalInformation;
    }

    public String getFirstName() {
        return personalInformation != null ? personalInformation.getFirstName() : null;
    }

    public String getLastName() {
        return personalInformation != null ? personalInformation.getLastName() : null;
    }

    public LocalDate getDateOfBirth() {
        return personalInformation != null ? personalInformation.getDateOfBirth() : null;
    }

    public String getNationality() {
        return personalInformation != null ? personalInformation.getNationalityCode() : null;
    }

    public Gender getGender() {
        return personalInformation != null ? personalInformation.getGender() : null;
    }

    public Address getAddress() {
        return address;
    }

    public EmailAddress getEmail() {
        return email;
    }

    public PhoneNumber getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    public String getChipNumber() {
        return chipNumber;
    }

    public IdentityCard getIdentityCard() {
        return identityCard;
    }

    public MedicalCourse getMedicalCourse() {
        return medicalCourse;
    }

    public TrainerLicense getTrainerLicense() {
        return trainerLicense;
    }

    public RefereeLicense getRefereeLicense() {
        return refereeLicense;
    }

    public DrivingLicenseGroup getDrivingLicenseGroup() {
        return drivingLicenseGroup;
    }

    public String getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public BirthNumber getBirthNumber() {
        return birthNumber;
    }

    public BankAccountNumber getBankAccountNumber() {
        return bankAccountNumber;
    }

    public DeactivationReason getSuspensionReason() {
        return suspensionReason;
    }

    public Instant getSuspendedAt() {
        return suspendedAt;
    }

    public String getSuspensionNote() {
        return suspensionNote;
    }

    public UserId getSuspendedBy() {
        return suspendedBy;
    }

    // ========== Command Handlers (Domain Methods) ==========

    /**
     * Applies a full end-state snapshot: every field of the command is written as-is. Callers that
     * only mean to touch some fields pass {@link UpdateMember#from(Member)} as the baseline and
     * overlay just those — a field left at its baseline value round-trips to the same value here.
     */
    public void update(UpdateMember command, GuardianContacts guardians) {
        PersonalInformation newPersonalInfo = PersonalInformation.of(
                command.firstName(), command.lastName(), command.dateOfBirth(),
                command.nationality(), command.gender());

        BirthNumber newBirthNumber = command.birthNumber();
        if (newBirthNumber != null && !newPersonalInfo.getNationality().isCzech()) {
            newBirthNumber = null;
        }
        // Consistency rule: always enforced, regardless of completeness
        validateBirthNumberConsistency(newPersonalInfo.getNationalityCode(), newBirthNumber);

        // Never-worsen rule: an edit may only fill in missing data, never add to it (design.md D5). The one
        // exception is a corrected date of birth turning an adult into a minor, who then lacks a guardian.
        Set<MissingDataItem> missingBefore = MemberCompleteness.missingData(this, guardians);
        Set<MissingDataItem> missingAfter = MemberCompleteness.missingData(command.email(), command.phone(),
                newPersonalInfo, newBirthNumber, command.address(), guardians);
        boolean becameMinor = !personalInformation.isMinor() && newPersonalInfo.isMinor();
        enforceNeverWorsen(missingBefore, missingAfter, becameMinor);
        recordMissingData(missingAfter);

        BirthNumber previousBirthNumber = this.birthNumber;

        this.email = command.email();
        this.phone = command.phone();
        this.address = command.address();
        this.personalInformation = newPersonalInfo;
        this.birthNumber = newBirthNumber;
        this.chipNumber = command.chipNumber();
        this.bankAccountNumber = command.bankAccountNumber();
        this.identityCard = command.identityCard();
        this.drivingLicenseGroup = command.drivingLicenseGroup();
        this.medicalCourse = command.medicalCourse();
        this.trainerLicense = command.trainerLicense();
        this.refereeLicense = command.refereeLicense();
        this.dietaryRestrictions = command.dietaryRestrictions();

        if (command.updatedBy() != null && !Objects.equals(this.birthNumber, previousBirthNumber)) {
            registerEvent(BirthNumberAccessedEvent.modified(command.updatedBy(), this.id));
        }
    }

    /**
     * Applies an inward write from ORIS synchronisation, overwriting every ORIS-owned field.
     * <p>
     * Distinct from {@link #update(UpdateMember)} on purpose (design.md D3): a synchronisation has
     * no {@link UserId} to attribute a birth-number access to, so this method does not publish
     * {@link BirthNumberAccessedEvent} even when the birth number changes. It also cannot touch
     * licences, bank account, dietary requirements or the suspension block — those fields
     * are simply absent from {@link SyncFromOris}. Whether ORIS's field-by-field merge protection
     * has already run is decided by the caller before this method is invoked.
     *
     * @param command sync command with all ORIS-sourced fields
     */
    public void syncFromOris(SyncFromOris command) {
        Assert.isTrue(Objects.equals(this.registrationNumber, command.registrationNumber()),
                "SyncFromOris command targets a different member");

        PersonalInformation newPersonalInfo = PersonalInformation.of(
                command.firstName(), command.lastName(), command.dateOfBirth(),
                command.nationality() != null ? command.nationality().code() : null,
                command.gender());

        // Completeness rules are dropped for ORIS-owned fields (design.md D5): ORIS is the
        // authority, so a synchronisation may leave the member incomplete rather than being
        // refused or holding on to a stale value. Only the consistency rule survives — a birth
        // number is never accepted for a non-Czech national.
        validateBirthNumberConsistency(newPersonalInfo.getNationalityCode(), command.birthNumber());

        this.personalInformation = newPersonalInfo;
        this.birthNumber = command.birthNumber();
        this.email = command.email();
        this.phone = command.phone();
        this.address = command.address();
        this.chipNumber = command.chipNumber();
    }

    /**
     * Handles SuspendMembership command.
     * <p>
     * Suspends a member's membership with the specified reason.
     * This method modifies the Member in-place (mutable pattern).
     * <p>
     * Enforces the business rule that an already suspended member cannot be suspended again.
     *
     * @param command the suspension command
     * @throws BusinessRuleViolationException if member is already suspended
     */
    public void suspend(SuspendMembership command) {
        if (!this.active) {
            throw new BusinessRuleViolationException(
                    "Member is already suspended and cannot be suspended again"
            ) {};
        }

        Objects.requireNonNull(command.reason(), "Suspension reason is required");

        this.active = false;
        this.suspensionReason = command.reason();
        this.suspendedAt = Instant.now();
        this.suspensionNote = command.note();
        this.suspendedBy = command.suspendedBy();

        registerEvent(MemberSuspendedEvent.fromAggregate(this, command));
    }

    /**
     * Handles ResumeMembership command.
     * <p>
     * Resumes a suspended member's membership. This clears the suspension fields
     * and sets the member back to active status.
     * <p>
     * Enforces the business rule that an already active member cannot be resumed.
     *
     * @param command the resume command
     * @throws BusinessRuleViolationException if member is already active
     */
    public void resume(ResumeMembership command) {
        if (this.active) {
            throw new BusinessRuleViolationException(
                    "Member is already active and cannot be resumed"
            ) {};
        }

        Objects.requireNonNull(command.resumedBy(), "Resumed by user is required");

        this.active = true;
        this.suspensionReason = null;
        this.suspendedAt = null;
        this.suspensionNote = null;
        this.suspendedBy = null;

        registerEvent(MemberResumedEvent.fromAggregate(this, command));
    }

    /**
     * Checks for inconsistencies between the stored birth number and the member's date of birth and gender.
     * Returns warnings (not errors) — the data is already persisted; these are advisory notices.
     *
     * @return list of warning messages, empty when birth number is absent or fully consistent
     */
    public List<String> birthNumberConsistencyWarnings() {
        if (birthNumber == null) {
            return Collections.emptyList();
        }

        LocalDate dateOfBirth = getDateOfBirth();
        Gender gender = getGender();

        if (dateOfBirth == null || gender == null) {
            return Collections.emptyList();
        }

        List<String> warnings = new ArrayList<>();

        LocalDate encodedDate = birthNumber.extractDate(dateOfBirth.getYear());
        if (!encodedDate.equals(dateOfBirth)) {
            warnings.add("Birth number date (%02d.%02d.%d) does not match member's date of birth".formatted(
                    encodedDate.getDayOfMonth(), encodedDate.getMonthValue(), encodedDate.getYear()));
        }

        Gender indicatedGender = birthNumber.indicatesGender();
        if (indicatedGender != gender) {
            warnings.add("Birth number indicates different gender than selected");
        }

        return Collections.unmodifiableList(warnings);
    }

    @Override
    public String toString() {
        return "Member{" +
               "id=" + getId() +
               ", firstName='" + getFirstName() + '\'' +
               ", lastName='" + getLastName() + '\'' +
               ", dateOfBirth=" + getDateOfBirth() +
               ", nationality='" + getNationality() + '\'' +
               ", active=" + active +
               '}';
    }
}
