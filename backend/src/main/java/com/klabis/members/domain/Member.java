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
import java.util.List;
import java.util.Objects;

/**
 * Member aggregate root.
 * <p>
 * Represents a club member with personal information, contacts, and registration details.
 * This is the aggregate root for the Members bounded context.
 * <p>
 * Business invariants:
 * - Registration number must be unique
 * - At least one email and one phone required (member OR guardian)
 * - Rodne cislo only allowed for Czech nationality
 * - Guardian required for minors (<18 years)
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
    private GuardianInformation guardian;
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
            GuardianInformation guardian,
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
                    member.guardian,
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
     * Fields set to null retain the current value (PATCH semantics).
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
            GuardianInformation guardian,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Gender gender,
            BirthNumber birthNumber,
            UserId updatedBy
    ) {
        public static UpdateMember from(Member member) {
            return new UpdateMember(
                    member.email,
                    member.phone,
                    member.address,
                    member.chipNumber,
                    member.personalInformation != null ? member.personalInformation.getNationalityCode() : null,
                    member.bankAccountNumber,
                    member.identityCard,
                    member.drivingLicenseGroup,
                    member.medicalCourse,
                    member.trainerLicense,
                    member.refereeLicense,
                    member.dietaryRestrictions,
                    member.guardian,
                    member.personalInformation != null ? member.personalInformation.getFirstName() : null,
                    member.personalInformation != null ? member.personalInformation.getLastName() : null,
                    member.personalInformation != null ? member.personalInformation.getDateOfBirth() : null,
                    member.personalInformation != null ? member.personalInformation.getGender() : null,
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

    // ========== Constructors ==========

    private Member(
            MemberId id,
            RegistrationNumber registrationNumber,
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            GuardianInformation guardian,
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
        this.guardian = guardian;
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
     * @param guardian            guardian information (may be null)
     * @param active              whether the member is active
     * @param chipNumber          member's chip number (may be null)
     * @param identityCard        member's identity card (may be null)
     * @param medicalCourse       member's medical course (may be null)
     * @param trainerLicense      member's trainer license (may be null)
     * @param drivingLicenseGroup member's driving license group (may be null)
     * @param dietaryRestrictions member's dietary restrictions (may be null)
     * @param birthNumber         member's birth number (may be null)
     * @param bankAccountNumber   member's bank account number (may be null)
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
            GuardianInformation guardian,
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
                guardian,
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
                suspensionReason,
                suspendedAt,
                suspensionNote,
                suspendedBy
        );
        member.updateAuditMetadata(auditMetadata);
        // No domain events for reconstructed entities
        return member;
    }

    public static Member register(RegisterMember command) {
        // Validate required fields
        Assert.notNull(command.id(), "Member ID is required");
        Assert.notNull(command.registrationNumber(), "Registration number is required");
        Assert.notNull(command.personalInformation(), "Personal information is required");
        Assert.notNull(command.address(), "Address is required");

        // Validate contact information
        validateContactInformation(command.email(), command.phone(), command.guardian());

        // Validate guardian for minors
        validateGuardianForMinors(command.personalInformation(), command.guardian());

        // Validate birth number nationality
        validateBirthNumberNationality(command.personalInformation().getNationalityCode(), command.birthNumber());

        Member member = new Member(
                command.id(),
                command.registrationNumber(),
                command.personalInformation(),
                command.address(),
                command.email(),
                command.phone(),
                command.guardian(),
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

    private static void validateContactInformation(
            EmailAddress email,
            PhoneNumber phone,
            GuardianInformation guardian) {

        boolean memberHasEmail = email != null;
        boolean memberHasPhone = phone != null;

        // GuardianInformation enforces non-null email and phone in its constructor
        // Therefore, if guardian is present, it always has both email and phone
        boolean guardianHasEmail = guardian != null; // guardian.getEmail() never null
        boolean guardianHasPhone = guardian != null; // guardian.getPhone() never null

        boolean hasEmail = memberHasEmail || guardianHasEmail;
        boolean hasPhone = memberHasPhone || guardianHasPhone;

        Assert.isTrue(hasEmail, "At least one email address is required (member or guardian)");
        Assert.isTrue(hasPhone, "At least one phone number is required (member or guardian)");
    }

    /**
     * Validates that a guardian is provided for minor members.
     *
     * <p><b>Business Rule:</b> Guardian contact information is required when
     * creating or updating a Member who is a minor (under 18) at the time of
     * the operation. The age is calculated dynamically based on the current date.
     *
     * <p>Once a guardian is assigned, that relationship is permanent and does not
     * expire when the member turns 18 later.
     *
     * @param dateOfBirth the member's date of birth
     * @param guardian    the guardian information (may be null for adults)
     * @throws IllegalArgumentException if member is under 18 and no guardian is provided
     */
    private static void validateGuardianForMinors(
            PersonalInformation personalInformation,
            GuardianInformation guardian) {

        if (personalInformation.isMinor() && guardian == null) {
            throw new BusinessRuleViolationException(
                    "Guardian is required for minors (under 18 years)"
            ) {
            };
        }
    }

    /**
     * Validates birth number and nationality consistency.
     *
     * <p><b>Business Rule:</b> Birth number (rodné číslo) is required for Czech nationals
     * and forbidden for non-Czech nationals.
     *
     * @param nationalityCode the member's nationality code (ISO 3166-1)
     * @param birthNumber     the birth number to validate (may be null)
     * @throws BusinessRuleViolationException if birth number is provided for non-Czech nationality
     *                                        or missing for Czech nationality
     */
    private static void validateBirthNumberNationality(String nationalityCode, BirthNumber birthNumber) {
        Nationality nationality = Nationality.of(nationalityCode);

        if (birthNumber != null && !nationality.isCzech()) {
            throw new BusinessRuleViolationException(
                    "Birth number is only allowed for Czech nationals"
            ) {
            };
        }

        if (birthNumber == null && nationality.isCzech()) {
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

    public GuardianInformation getGuardian() {
        return guardian;
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

    public void update(UpdateMember command) {
        EmailAddress newEmail = command.email() != null ? command.email() : this.email;
        PhoneNumber newPhone = command.phone() != null ? command.phone() : this.phone;
        Address newAddress = command.address() != null ? command.address() : this.address;
        GuardianInformation newGuardian = command.guardian() != null ? command.guardian() : this.guardian;

        validateContactInformation(newEmail, newPhone, newGuardian);

        boolean anyPersonalFieldChanged = command.firstName() != null || command.lastName() != null
                || command.dateOfBirth() != null || command.nationality() != null || command.gender() != null;

        PersonalInformation newPersonalInfo;
        if (anyPersonalFieldChanged) {
            String newFirstName = command.firstName() != null ? command.firstName() : this.personalInformation.getFirstName();
            String newLastName = command.lastName() != null ? command.lastName() : this.personalInformation.getLastName();
            LocalDate newDateOfBirth = command.dateOfBirth() != null ? command.dateOfBirth() : this.personalInformation.getDateOfBirth();
            Gender newGender = command.gender() != null ? command.gender() : this.personalInformation.getGender();
            String newNationality = command.nationality() != null ? command.nationality() : this.personalInformation.getNationalityCode();
            newPersonalInfo = PersonalInformation.of(newFirstName, newLastName, newDateOfBirth, newNationality, newGender);
        } else {
            newPersonalInfo = this.personalInformation;
        }

        validateGuardianForMinors(newPersonalInfo, newGuardian);

        BirthNumber newBirthNumber = command.birthNumber() != null ? command.birthNumber() : this.birthNumber;
        if (newBirthNumber != null && !Nationality.of(newPersonalInfo.getNationalityCode()).isCzech()) {
            newBirthNumber = null;
        }
        validateBirthNumberNationality(newPersonalInfo.getNationalityCode(), newBirthNumber);

        BirthNumber previousBirthNumber = this.birthNumber;

        this.email = newEmail;
        this.phone = newPhone;
        this.address = newAddress;
        this.guardian = newGuardian;
        this.personalInformation = newPersonalInfo;
        this.birthNumber = newBirthNumber;

        if (command.chipNumber() != null) this.chipNumber = command.chipNumber();
        if (command.bankAccountNumber() != null) this.bankAccountNumber = command.bankAccountNumber();
        if (command.identityCard() != null) this.identityCard = command.identityCard();
        if (command.drivingLicenseGroup() != null) this.drivingLicenseGroup = command.drivingLicenseGroup();
        if (command.medicalCourse() != null) this.medicalCourse = command.medicalCourse();
        if (command.trainerLicense() != null) this.trainerLicense = command.trainerLicense();
        if (command.refereeLicense() != null) this.refereeLicense = command.refereeLicense();
        if (command.dietaryRestrictions() != null) this.dietaryRestrictions = command.dietaryRestrictions();

        if (command.birthNumber() != null && command.updatedBy() != null && !command.birthNumber().equals(previousBirthNumber)) {
            registerEvent(BirthNumberAccessedEvent.modified(command.updatedBy(), this.id));
        }
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
