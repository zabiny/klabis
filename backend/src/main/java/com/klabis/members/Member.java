package com.klabis.members;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.exceptions.BusinessRuleViolationException;
import com.klabis.users.UserId;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.DomainEvents;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

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
public class Member {

    @Identity
    private final UserId id;
    private RegistrationNumber registrationNumber;

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

    // Audit metadata
    private AuditMetadata auditMetadata;

    // Domain events list (published synchronously in same thread, no concurrent access)
    private final List<Object> domainEvents = new ArrayList<>();

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

    // ========== Constructors ==========

    /**
     * Private constructor for creating new Member instances.
     * <p>
     * This constructor is used by the static factory methods (create, reconstruct, createWithId)
     * to ensure business invariants are validated during construction.
     */
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
            BankAccountNumber bankAccountNumber) {

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
        this.drivingLicenseGroup = drivingLicenseGroup;
        this.dietaryRestrictions = dietaryRestrictions;
        this.birthNumber = birthNumber;
        this.bankAccountNumber = bankAccountNumber;
    }

    /**
     * Factory method for reconstructing Member from persistence layer.
     * This bypasses validation since the data was already validated when originally stored.
     * <p>
     * This method is public only for infrastructure/persistence layer usage.
     * Use {@link #create()} for creating new members.
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
                bankAccountNumber
        );
        member.auditMetadata = auditMetadata;
        // No domain events for reconstructed entities
        return member;
    }

    public static Member register(RegisterMember command) {
        return Member.createWithId(
                command.id(),
                command.registrationNumber(),
                command.personalInformation(),
                command.address(),
                command.email(),
                command.phone(),
                command.guardian(),
                command.birthNumber(),
                command.bankAccountNumber()
        );
    }

    /**
     * Static factory method to create a new Member with a specific ID.
     * <p>
     * This method is used when the Member ID needs to be shared with another aggregate
     * (e.g., User aggregate) to ensure both aggregates use the same identifier.
     *
     * @param id                  member's unique identifier (typically shared with User)
     * @param registrationNumber  unique registration number
     * @param personalInformation member's personal information
     * @param address             member's address
     * @param email               member's email address (may be null if guardian has email)
     * @param phone               member's phone number (may be null if guardian has phone)
     * @param guardian            guardian information (required for minors)
     * @param birthNumber         birth number (only for Czech nationals)
     * @param bankAccountNumber   bank account number
     * @return new Member instance with the specified ID
     * @throws IllegalArgumentException if business rules are violated
     */
    public static Member createWithId(
            UserId id,
            RegistrationNumber registrationNumber,
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            GuardianInformation guardian,
            BirthNumber birthNumber,
            BankAccountNumber bankAccountNumber) {

        // Validate required fields
        Objects.requireNonNull(id, "Member ID is required");
        Objects.requireNonNull(registrationNumber, "Registration number is required");
        Objects.requireNonNull(personalInformation, "Personal information is required");
        Objects.requireNonNull(address, "Address is required");

        // Validate contact information
        validateContactInformation(email, phone, guardian);

        // Validate guardian for minors
        validateGuardianForMinors(personalInformation.getDateOfBirth(), guardian);

        // Validate birth number nationality
        validateBirthNumberNationality(personalInformation.getNationalityCode(), birthNumber);

        Member member = new Member(
                id,
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                guardian,
                true, // new members are active by default
                null, // chipNumber
                null, // identityCard
                null, // medicalCourse
                null, // trainerLicense
                null, // drivingLicenseGroup
                null, // dietaryRestrictions
                birthNumber,
                bankAccountNumber
        );

        // Register domain event
        member.registerEvent(MemberCreatedEvent.fromMember(member));

        return member;
    }

    /**
     * Static factory method to create a new Member.
     * <p>
     * This method generates a new random UUID for the Member ID.
     * Use {@link #createWithId(UserId, RegistrationNumber, PersonalInformation, Address, EmailAddress, PhoneNumber, GuardianInformation)}
     * when you need to specify the ID (e.g., to share with User aggregate).
     *
     * @param registrationNumber  unique registration number
     * @param personalInformation member's personal information
     * @param address             member's address
     * @param email               member's email address (may be null if guardian has email)
     * @param phone               member's phone number (may be null if guardian has phone)
     * @param guardian            guardian information (required for minors)
     * @return new Member instance
     * @throws IllegalArgumentException if business rules are violated
     * @deprecated use @{@link Member#register(RegisterMember)}
     */
    @Deprecated
    public static Member create(
            RegistrationNumber registrationNumber,
            PersonalInformation personalInformation,
            Address address,
            EmailAddress email,
            PhoneNumber phone,
            GuardianInformation guardian) {

        // Validate required fields
        Objects.requireNonNull(registrationNumber, "Registration number is required");
        Objects.requireNonNull(personalInformation, "Personal information is required");
        Objects.requireNonNull(address, "Address is required");

        // Validate contact information
        validateContactInformation(email, phone, guardian);

        // Validate guardian for minors
        validateGuardianForMinors(personalInformation.getDateOfBirth(), guardian);

        Member member = new Member(
                new UserId(UUID.randomUUID()),
                registrationNumber,
                personalInformation,
                address,
                email,
                phone,
                guardian,
                true, // new members are active by default
                null, // chipNumber
                null, // identityCard
                null, // medicalCourse
                null, // trainerLicense
                null, // drivingLicenseGroup
                null, // dietaryRestrictions
                null, // birthNumber
                null  // bankAccountNumber
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

    // Audit getters (delegate to AuditMetadata)
    public Instant getCreatedAt() {
        return auditMetadata != null ? auditMetadata.createdAt() : null;
    }

    public Instant getLastModifiedAt() {
        return auditMetadata != null ? auditMetadata.lastModifiedAt() : null;
    }

    public String getCreatedBy() {
        return auditMetadata != null ? auditMetadata.createdBy() : null;
    }

    public String getLastModifiedBy() {
        return auditMetadata != null ? auditMetadata.lastModifiedBy() : null;
    }

    public Long getVersion() {
        return auditMetadata != null ? auditMetadata.version() : null;
    }

    public AuditMetadata getAuditMetadata() {
        return auditMetadata;
    }

    /**
     * Register a domain event to be published.
     *
     * @param event the domain event to register
     */
    protected void registerEvent(Object event) {
        this.domainEvents.add(event);
    }

    /**
     * Get all domain events registered on this aggregate.
     * <p>
     * Public accessor that returns the domain events list.
     * Annotated with @DomainEvents for Spring Modulith automatic event publishing.
     *
     * @return unmodifiable list of domain events
     */
    @DomainEvents
    public List<Object> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clear all domain events (typically called after publishing).
     */
    public void clearDomainEvents() {
        this.domainEvents.clear();
    }

    /**
     * Get the member's unique identifier.
     *
     * @return the member's ID as a UserId value object
     */
    public UserId getId() {
        return this.id;
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
     * @deprecated Use {@link #handle(UpdateContactInformation)} instead.
     * This method is kept for backward compatibility during migration.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void updateContactInformation(EmailAddress email, PhoneNumber phone, Address address) {
        handle(new UpdateContactInformation(email, phone, address));
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
     * @deprecated Use {@link #handle(UpdateDocuments)} instead.
     * This method is kept for backward compatibility during migration.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void updateDocuments(
            IdentityCard identityCard,
            MedicalCourse medicalCourse,
            TrainerLicense trainerLicense) {
        handle(new UpdateDocuments(identityCard, medicalCourse, trainerLicense));
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
     * @deprecated Use {@link #handle(UpdateMemberDetails)} instead.
     * This method is kept for backward compatibility during migration.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public void updateMemberDetails(
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
            Gender gender) {
        handle(new UpdateMemberDetails(
                personalInformation,
                address,
                email,
                phone,
                guardian,
                chipNumber,
                drivingLicenseGroup,
                dietaryRestrictions,
                birthNumber,
                bankAccountNumber,
                gender
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return Objects.equals(getId(), member.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
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
