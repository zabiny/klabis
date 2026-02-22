package com.klabis.members.domain;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.domain.KlabisAggregateRoot;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.members.MemberCreatedEvent;
import com.klabis.members.MemberTerminatedEvent;
import com.klabis.users.UserId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;
import java.util.UUID;

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
    private DrivingLicenseGroup drivingLicenseGroup;
    private String dietaryRestrictions;
    private BirthNumber birthNumber;
    private BankAccountNumber bankAccountNumber;

    // Termination fields
    private DeactivationReason deactivationReason;
    private Instant deactivatedAt;
    private String deactivationNote;
    private String deactivatedBy;

    // ========== Command Records ==========

    /**
     * Command to register a new member with a specific ID.
     * <p>
     * This command is used when the Member ID needs to be shared with another aggregate
     * (e.g., User aggregate) to ensure both aggregates use the same identifier.
     */
    public record RegisterMember(
            UserId id,
            RegistrationNumber registrationNumber,
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            GuardianInformation guardian,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber
    ) {}

    /**
     * Command to update member contact information.
     * <p>
     * This command allows members to update their own contact details.
     * Fields set to null will retain existing values (PATCH semantics).
     */
    public record UpdateContactInformation(
            EmailAddress email,
            PhoneNumber phone,
            Address address
    ) {}

    /**
     * Command to update member documents.
     * <p>
     * Admin-only command for updating identity card, medical course, and trainer license.
     * Fields set to null will retain existing values (PATCH semantics).
     */
    public record UpdateDocuments(
            IdentityCard identityCard,
            MedicalCourse medicalCourse,
            TrainerLicense trainerLicense
    ) {}

    /**
     * Command to update admin-only member details.
     * <p>
     * This command allows administrators to update personal information,
     * contact details, guardian info, and extended member attributes.
     * Fields set to null will retain existing values (PATCH semantics).
     */
    public record UpdateMemberDetails(
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            GuardianInformation guardian,
            String chipNumber,
            DrivingLicenseGroup drivingLicenseGroup,
            String dietaryRestrictions,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber,
            Gender gender
    ) {}

    /**
     * Command for a member editing their own profile.
     * <p>
     * Contains only fields that a member is permitted to change on their own account.
     * Fields set to null retain the current value (PATCH semantics).
     */
    public record SelfUpdate(
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
            String dietaryRestrictions,
            GuardianInformation guardian
    ) {}

    /**
     * Command for an administrator updating any member's profile.
     * <p>
     * Includes all self-editable fields plus fields that are restricted to admins:
     * firstName, lastName, dateOfBirth, gender, and birthNumber.
     * Fields set to null retain the current value (PATCH semantics).
     */
    public record UpdateMemberByAdmin(
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
            String dietaryRestrictions,
            GuardianInformation guardian,
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            Gender gender,
            BirthNumber birthNumber
    ) {}

    /**
     * Command to terminate a member's membership.
     * <p>
     * This command is used by administrators to terminate a member's membership
     * with a specific reason and optional note.
     */
    public record TerminateMembership(
            UserId terminatedBy,
            DeactivationReason reason,
            String note
    ) {}

    // ========== Constructors ==========

    private Member(
            UserId id,
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
            DrivingLicenseGroup drivingLicenseGroup,
            String dietaryRestrictions,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber,
            DeactivationReason deactivationReason,
            Instant deactivatedAt,
            String deactivationNote,
            String deactivatedBy) {

        this.id = MemberId.fromUserId(id);
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
        this.drivingLicenseGroup = drivingLicenseGroup;
        this.dietaryRestrictions = dietaryRestrictions;
        this.birthNumber = birthNumber;
        this.bankAccountNumber = bankAccountNumber;
        this.deactivationReason = deactivationReason;
        this.deactivatedAt = deactivatedAt;
        this.deactivationNote = deactivationNote;
        this.deactivatedBy = deactivatedBy;
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
     * @param deactivationReason  reason for deactivation (may be null)
     * @param deactivatedAt       timestamp of deactivation (may be null)
     * @param deactivationNote    optional deactivation note (may be null)
     * @param deactivatedBy       user who deactivated (may be null)
     * @return reconstructed Member instance
     */
    public static Member reconstruct(
            UserId id,
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
            DrivingLicenseGroup drivingLicenseGroup,
            String dietaryRestrictions,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber,
            DeactivationReason deactivationReason,
            Instant deactivatedAt,
            String deactivationNote,
            String deactivatedBy,
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
                drivingLicenseGroup,
                dietaryRestrictions,
                birthNumber,
                bankAccountNumber,
                deactivationReason,
                deactivatedAt,
                deactivationNote,
                deactivatedBy
        );
        member.updateAuditMetadata(auditMetadata);
        // No domain events for reconstructed entities
        return member;
    }

    public static Member register(RegisterMember command) {
        // Validate required fields
        Objects.requireNonNull(command.id(), "Member ID is required");
        Objects.requireNonNull(command.registrationNumber(), "Registration number is required");
        Objects.requireNonNull(command.personalInformation(), "Personal information is required");
        Objects.requireNonNull(command.address(), "Address is required");

        // Validate contact information
        validateContactInformation(command.email(), command.phone(), command.guardian());

        // Validate guardian for minors
        validateGuardianForMinors(command.personalInformation().getDateOfBirth(), command.guardian());

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
                null, // drivingLicenseGroup
                null, // dietaryRestrictions
                command.birthNumber(),
                command.bankAccountNumber(),
                null, // deactivationReason
                null, // deactivatedAt
                null, // deactivationNote
                null  // deactivatedBy
        );

        // Register domain event
        member.registerEvent(MemberCreatedEvent.fromMember(member));

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

        // At least one email and one phone required (from member OR guardian)
        boolean hasEmail = memberHasEmail || guardianHasEmail;
        boolean hasPhone = memberHasPhone || guardianHasPhone;

        // If both missing, throw combined error
        if (!hasEmail && !hasPhone) {
            throw new IllegalArgumentException(
                    "At least one email and one phone required (member or guardian)"
            );
        }

        // Otherwise check individually
        if (!hasEmail) {
            throw new IllegalArgumentException(
                    "At least one email is required (member or guardian)"
            );
        }

        if (!hasPhone) {
            throw new IllegalArgumentException(
                    "At least one phone is required (member or guardian)"
            );
        }
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
            LocalDate dateOfBirth,
            GuardianInformation guardian) {

        int age = Period.between(dateOfBirth, LocalDate.now()).getYears();
        boolean isMinor = age < 18;

        if (isMinor && guardian == null) {
            throw new BusinessRuleViolationException(
                    "Guardian is required for minors (under 18 years)"
            ) {
            };
        }
    }

    /**
     * Validates that birth number is only provided for Czech nationals.
     *
     * <p><b>Business Rule:</b> Birth number (rodné číslo) is a Czech-specific identifier
     * and should only be stored for members with Czech nationality.
     *
     * @param nationalityCode the member's nationality code (ISO 3166-1)
     * @param birthNumber     the birth number to validate (may be null)
     * @throws IllegalArgumentException if birth number is provided for non-Czech nationality
     */
    private static void validateBirthNumberNationality(String nationalityCode, BirthNumber birthNumber) {
        if (birthNumber == null) {
            return;
        }

        boolean isCzech = "CZ".equals(nationalityCode) || "CZE".equals(nationalityCode);
        if (!isCzech) {
            throw new BusinessRuleViolationException(
                    "Birth number is only allowed for Czech nationals"
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

    public DeactivationReason getDeactivationReason() {
        return deactivationReason;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public String getDeactivationNote() {
        return deactivationNote;
    }

    public UserId getDeactivatedBy() {
        return deactivatedBy != null ? new UserId(UUID.fromString(deactivatedBy)) : null;
    }

    // ========== Command Handlers (Domain Methods) ==========

    /**
     * Handles UpdateContactInformation command.
     * <p>
     * Updates member-editable contact information.
     * This method modifies the Member in-place (mutable pattern).
     * <p>
     * Enforces the business rule that at least one email and one phone
     * must exist after the update (member OR guardian).
     *
     * @param command the update contact information command
     * @throws IllegalArgumentException if contact validation fails
     */
    public void handle(UpdateContactInformation command) {
        // Use provided values or fall back to existing values
        EmailAddress newEmail = (command.email() != null) ? command.email() : this.email;
        PhoneNumber newPhone = (command.phone() != null) ? command.phone() : this.phone;
        Address newAddress = (command.address() != null) ? command.address() : this.address;

        // Validate contact information (at least one email and one phone required)
        validateContactInformation(newEmail, newPhone, this.guardian);

        // Modify fields in-place
        this.email = newEmail;
        this.phone = newPhone;
        this.address = newAddress;
    }


    /**
     * Handles UpdateDocuments command.
     * <p>
     * Updates member documents (identity card, medical course, trainer license).
     * This method modifies the Member in-place (mutable pattern).
     * <p>
     * Document expiry validation happens automatically when creating new
     * ExpiringDocument instances (IdentityCard, TrainerLicense).
     *
     * @param command the update documents command
     */
    public void handle(UpdateDocuments command) {
        // Modify fields in-place
        if (command.identityCard() != null) {
            this.identityCard = command.identityCard();
        }
        if (command.medicalCourse() != null) {
            this.medicalCourse = command.medicalCourse();
        }
        if (command.trainerLicense() != null) {
            this.trainerLicense = command.trainerLicense();
        }
    }


    /**
     * Handles UpdateMemberDetails command.
     * <p>
     * Updates admin-only fields and extended member information.
     * This method modifies the Member in-place (mutable pattern).
     * These fields are restricted to users with MEMBERS:UPDATE authority.
     *
     * @param command the update member details command
     * @throws IllegalArgumentException if validation fails
     */
    public void handle(UpdateMemberDetails command) {
        // If gender is provided, we need to update personalInformation with new gender
        PersonalInformation newPersonalInfo = this.personalInformation;
        if (command.gender() != null) {
            if (command.personalInformation() != null) {
                // Both provided - use the new personalInformation
                newPersonalInfo = command.personalInformation();
            } else {
                // Only gender provided - reconstruct personalInformation with new gender
                newPersonalInfo = PersonalInformation.of(
                        this.personalInformation.getFirstName(),
                        this.personalInformation.getLastName(),
                        this.personalInformation.getDateOfBirth(),
                        this.personalInformation.getNationalityCode(),
                        command.gender()
                );
            }
        } else if (command.personalInformation() != null) {
            newPersonalInfo = command.personalInformation();
        }

        Address newAddress = (command.address() != null) ? command.address() : this.address;
        EmailAddress newEmail = (command.email() != null) ? command.email() : this.email;
        PhoneNumber newPhone = (command.phone() != null) ? command.phone() : this.phone;
        GuardianInformation newGuardian = (command.guardian() != null) ? command.guardian() : this.guardian;

        // Validate contact information
        validateContactInformation(newEmail, newPhone, newGuardian);

        // Validate guardian for minors if guardian is being updated
        if (command.guardian() != null || newPersonalInfo != this.personalInformation) {
            validateGuardianForMinors(newPersonalInfo.getDateOfBirth(), newGuardian);
        }

        // Validate birth number nationality if birth number is being updated
        BirthNumber newBirthNumber = (command.birthNumber() != null) ? command.birthNumber() : this.birthNumber;
        String nationalityCode = newPersonalInfo.getNationalityCode();
        validateBirthNumberNationality(nationalityCode, newBirthNumber);

        String newChipNumber = (command.chipNumber() != null) ? command.chipNumber() : this.chipNumber;
        DrivingLicenseGroup newDrivingLicenseGroup =
                (command.drivingLicenseGroup() != null) ? command.drivingLicenseGroup() : this.drivingLicenseGroup;
        String newDietaryRestrictions =
                (command.dietaryRestrictions() != null) ? command.dietaryRestrictions() : this.dietaryRestrictions;
        BankAccountNumber newBankAccountNumber =
                (command.bankAccountNumber() != null) ? command.bankAccountNumber() : this.bankAccountNumber;

        // Modify fields in-place
        this.personalInformation = newPersonalInfo;
        this.address = newAddress;
        this.email = newEmail;
        this.phone = newPhone;
        this.guardian = newGuardian;
        this.chipNumber = newChipNumber;
        this.drivingLicenseGroup = newDrivingLicenseGroup;
        this.dietaryRestrictions = newDietaryRestrictions;
        this.birthNumber = newBirthNumber;
        this.bankAccountNumber = newBankAccountNumber;
    }

    /**
     * Handles SelfUpdate command.
     * <p>
     * Applies changes to the subset of fields a member is allowed to edit on their own account.
     * Personal information fields (firstName, lastName, dateOfBirth, gender) and birthNumber
     * are intentionally excluded — those require admin authority.
     *
     * @param command the self-update command
     * @throws IllegalArgumentException if resulting contact information is invalid
     */
    public void handle(SelfUpdate command) {
        EmailAddress newEmail = command.email() != null ? command.email() : this.email;
        PhoneNumber newPhone = command.phone() != null ? command.phone() : this.phone;
        Address newAddress = command.address() != null ? command.address() : this.address;
        GuardianInformation newGuardian = command.guardian() != null ? command.guardian() : this.guardian;

        validateContactInformation(newEmail, newPhone, newGuardian);

        String newNationality = command.nationality() != null
                ? command.nationality()
                : this.personalInformation.getNationalityCode();
        PersonalInformation newPersonalInfo = PersonalInformation.of(
                this.personalInformation.getFirstName(),
                this.personalInformation.getLastName(),
                this.personalInformation.getDateOfBirth(),
                newNationality,
                this.personalInformation.getGender()
        );

        this.email = newEmail;
        this.phone = newPhone;
        this.address = newAddress;
        this.guardian = newGuardian;
        this.personalInformation = newPersonalInfo;

        if (command.chipNumber() != null) this.chipNumber = command.chipNumber();
        if (command.bankAccountNumber() != null) this.bankAccountNumber = command.bankAccountNumber();
        if (command.identityCard() != null) this.identityCard = command.identityCard();
        if (command.drivingLicenseGroup() != null) this.drivingLicenseGroup = command.drivingLicenseGroup();
        if (command.medicalCourse() != null) this.medicalCourse = command.medicalCourse();
        if (command.trainerLicense() != null) this.trainerLicense = command.trainerLicense();
        if (command.dietaryRestrictions() != null) this.dietaryRestrictions = command.dietaryRestrictions();
    }

    /**
     * Handles UpdateMemberByAdmin command.
     * <p>
     * Applies changes to all member fields, including those restricted to administrators
     * (firstName, lastName, dateOfBirth, gender, birthNumber).
     * Validates birth number against the resulting nationality.
     *
     * @param command the admin update command
     * @throws IllegalArgumentException if validation fails
     */
    public void handle(UpdateMemberByAdmin command) {
        EmailAddress newEmail = command.email() != null ? command.email() : this.email;
        PhoneNumber newPhone = command.phone() != null ? command.phone() : this.phone;
        Address newAddress = command.address() != null ? command.address() : this.address;
        GuardianInformation newGuardian = command.guardian() != null ? command.guardian() : this.guardian;

        validateContactInformation(newEmail, newPhone, newGuardian);

        String newFirstName = command.firstName() != null ? command.firstName() : this.personalInformation.getFirstName();
        String newLastName = command.lastName() != null ? command.lastName() : this.personalInformation.getLastName();
        LocalDate newDateOfBirth = command.dateOfBirth() != null ? command.dateOfBirth() : this.personalInformation.getDateOfBirth();
        Gender newGender = command.gender() != null ? command.gender() : this.personalInformation.getGender();
        String newNationality = command.nationality() != null ? command.nationality() : this.personalInformation.getNationalityCode();

        PersonalInformation newPersonalInfo = PersonalInformation.of(
                newFirstName, newLastName, newDateOfBirth, newNationality, newGender
        );

        BirthNumber newBirthNumber = command.birthNumber() != null ? command.birthNumber() : this.birthNumber;
        validateBirthNumberNationality(newNationality, newBirthNumber);

        if (command.guardian() != null || !newPersonalInfo.getDateOfBirth().equals(this.personalInformation.getDateOfBirth())) {
            validateGuardianForMinors(newPersonalInfo.getDateOfBirth(), newGuardian);
        }

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
        if (command.dietaryRestrictions() != null) this.dietaryRestrictions = command.dietaryRestrictions();
    }

    /**
     * Handles TerminateMembership command.
     * <p>
     * Terminates a member's membership with the specified reason.
     * This method modifies the Member in-place (mutable pattern).
     * <p>
     * Enforces the business rule that an already terminated member cannot be terminated again.
     *
     * @param command the termination command
     * @throws BusinessRuleViolationException if member is already terminated
     */
    public void handle(TerminateMembership command) {
        // Validate not already terminated
        if (!this.active) {
            throw new BusinessRuleViolationException(
                    "Member is already terminated and cannot be terminated again"
            ) {};
        }

        // Validate termination reason
        Objects.requireNonNull(command.reason(), "Termination reason is required");

        // Modify fields in-place
        this.active = false;
        this.deactivationReason = command.reason();
        this.deactivatedAt = Instant.now();
        this.deactivationNote = command.note();
        this.deactivatedBy = command.terminatedBy().uuid().toString();

        // Register domain event
        registerEvent(MemberTerminatedEvent.fromMember(this, command));
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
