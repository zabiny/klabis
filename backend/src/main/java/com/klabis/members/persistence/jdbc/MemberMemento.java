package com.klabis.members.persistence.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.members.*;
import com.klabis.users.UserId;
import org.springframework.data.annotation.*;
import org.springframework.data.domain.AfterDomainEventPublication;
import org.springframework.data.domain.DomainEvents;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Memento pattern implementation for Member aggregate persistence.
 * <p>
 * This class acts as a bridge between the pure domain {@link Member} entity
 * and Spring Data JDBC persistence. It contains:
 * <ul>
 *   <li>All JDBC annotations for persistence</li>
 *   <li>Flat primitive fields matching the database schema</li>
 *   <li>Conversion methods to/from Member</li>
 *   <li>Domain event delegation for Spring Modulith</li>
 * </ul>
 * <p>
 * The Member entity remains a pure domain object without Spring annotations,
 * while this memento handles all infrastructure concerns.
 */
@Table("members")
class MemberMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("registration_number")
    private String registrationNumber;

    // Personal information fields (flattened)
    @Column("first_name")
    private String firstName;

    @Column("last_name")
    private String lastName;

    @Column("date_of_birth")
    private LocalDate dateOfBirth;

    @Column("nationality")
    private String nationality;

    @Column("gender")
    private Gender gender;

    // Address fields (flattened)
    @Column("street")
    private String street;

    @Column("city")
    private String city;

    @Column("postal_code")
    private String postalCode;

    @Column("country")
    private String country;

    // Contact fields
    @Column("email")
    private String email;

    @Column("phone")
    private String phone;

    // Guardian fields (flattened)
    @Column("guardian_first_name")
    private String guardianFirstName;

    @Column("guardian_last_name")
    private String guardianLastName;

    @Column("guardian_relationship")
    private String guardianRelationship;

    @Column("guardian_email")
    private String guardianEmail;

    @Column("guardian_phone")
    private String guardianPhone;

    // Status and other fields
    @Column("is_active")
    private boolean active;

    @Column("chip_number")
    private String chipNumber;

    // Identity card fields (flattened)
    @Column("identity_card_number")
    private String identityCardNumber;

    @Column("identity_card_validity_date")
    private LocalDate identityCardValidityDate;

    // Medical course fields (flattened)
    @Column("medical_course_completion_date")
    private LocalDate medicalCourseCompletionDate;

    @Column("medical_course_validity_date")
    private LocalDate medicalCourseValidityDate;

    // Trainer license fields (flattened)
    @Column("trainer_license_number")
    private String trainerLicenseNumber;

    @Column("trainer_license_validity_date")
    private LocalDate trainerLicenseValidityDate;

    @Column("driving_license_group")
    private DrivingLicenseGroup drivingLicenseGroup;

    @Column("dietary_restrictions")
    private String dietaryRestrictions;

    // Audit fields
    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @CreatedBy
    @Column("created_by")
    private String createdBy;

    @LastModifiedDate
    @Column("modified_at")
    private Instant lastModifiedAt;

    @LastModifiedBy
    @Column("modified_by")
    private String lastModifiedBy;

    @Version
    @Column("version")
    private Long version;

    // Transient reference to Member for domain event delegation
    @Transient
    private Member member;

    // Transient flag for Persistable<UUID>
    @Transient
    private boolean isNew = true;

    /**
     * Default constructor required by Spring Data JDBC.
     */
    protected MemberMemento() {
    }

    /**
     * Creates a MemberMemento from a Member entity (for save operations).
     *
     * @param member the Member entity to convert
     * @return a new MemberMemento with all fields copied from the Member
     */
    public static MemberMemento from(Member member) {
        MemberMemento memento = new MemberMemento();

        copyIdAndRegistrationNumber(member, memento);
        copyPersonalInfo(member, memento);
        copyAddress(member, memento);
        copyContactInfo(member, memento);
        copyGuardian(member, memento);
        copyStatusFields(member, memento);
        copyIdentityCard(member, memento);
        copyMedicalCourse(member, memento);
        copyTrainerLicense(member, memento);
        copyOtherFields(member, memento);
        copyAuditMetadata(member, memento);

        // Store transient reference to Member for domain event delegation
        memento.member = member;

        // Set isNew flag based on whether member has audit metadata
        // New members (no audit metadata yet) -> INSERT (isNew = true)
        // Existing members (have audit metadata) -> UPDATE (isNew = false)
        memento.isNew = (member.getAuditMetadata() == null);

        return memento;
    }

    /**
     * Copies ID and registration number from Member to memento.
     */
    private static void copyIdAndRegistrationNumber(Member member, MemberMemento memento) {
        memento.id = member.getId() != null ? member.getId().uuid() : null;
        memento.registrationNumber = member.getRegistrationNumber() != null
                ? member.getRegistrationNumber().getValue()
                : null;
    }

    /**
     * Copies personal information fields from Member to memento.
     */
    private static void copyPersonalInfo(Member member, MemberMemento memento) {
        PersonalInformation personalInfo = member.getPersonalInformation();
        if (personalInfo != null) {
            memento.firstName = personalInfo.getFirstName();
            memento.lastName = personalInfo.getLastName();
            memento.dateOfBirth = personalInfo.getDateOfBirth();
            memento.nationality = personalInfo.getNationalityCode();
            memento.gender = personalInfo.getGender();
        }
    }

    /**
     * Copies address fields from Member to memento.
     */
    private static void copyAddress(Member member, MemberMemento memento) {
        Address address = member.getAddress();
        if (address != null) {
            memento.street = address.street();
            memento.city = address.city();
            memento.postalCode = address.postalCode();
            memento.country = address.country();
        }
    }

    /**
     * Copies contact information (email and phone) from Member to memento.
     */
    private static void copyContactInfo(Member member, MemberMemento memento) {
        memento.email = member.getEmail() != null ? member.getEmail().value() : null;
        memento.phone = member.getPhone() != null ? member.getPhone().value() : null;
    }

    /**
     * Copies guardian information from Member to memento.
     */
    private static void copyGuardian(Member member, MemberMemento memento) {
        GuardianInformation guardian = member.getGuardian();
        if (guardian != null) {
            memento.guardianFirstName = guardian.getFirstName();
            memento.guardianLastName = guardian.getLastName();
            memento.guardianRelationship = guardian.getRelationship();
            memento.guardianEmail = guardian.getEmailValue();
            memento.guardianPhone = guardian.getPhoneValue();
        }
    }

    /**
     * Copies status fields from Member to memento.
     */
    private static void copyStatusFields(Member member, MemberMemento memento) {
        memento.active = member.isActive();
        memento.chipNumber = member.getChipNumber();
    }

    /**
     * Copies identity card information from Member to memento.
     */
    private static void copyIdentityCard(Member member, MemberMemento memento) {
        IdentityCard identityCard = member.getIdentityCard();
        if (identityCard != null) {
            memento.identityCardNumber = identityCard.cardNumber();
            memento.identityCardValidityDate = identityCard.validityDate();
        }
    }

    /**
     * Copies medical course information from Member to memento.
     */
    private static void copyMedicalCourse(Member member, MemberMemento memento) {
        MedicalCourse medicalCourse = member.getMedicalCourse();
        if (medicalCourse != null) {
            memento.medicalCourseCompletionDate = medicalCourse.completionDate();
            memento.medicalCourseValidityDate = medicalCourse.validityDate().orElse(null);
        }
    }

    /**
     * Copies trainer license information from Member to memento.
     */
    private static void copyTrainerLicense(Member member, MemberMemento memento) {
        TrainerLicense trainerLicense = member.getTrainerLicense();
        if (trainerLicense != null) {
            memento.trainerLicenseNumber = trainerLicense.licenseNumber();
            memento.trainerLicenseValidityDate = trainerLicense.validityDate();
        }
    }

    /**
     * Copies other member fields from Member to memento.
     */
    private static void copyOtherFields(Member member, MemberMemento memento) {
        memento.drivingLicenseGroup = member.getDrivingLicenseGroup();
        memento.dietaryRestrictions = member.getDietaryRestrictions();
    }

    /**
     * Copies audit metadata from Member to memento.
     */
    private static void copyAuditMetadata(Member member, MemberMemento memento) {
        if (member.getAuditMetadata() != null) {
            memento.createdAt = member.getCreatedAt();
            memento.createdBy = member.getCreatedBy();
            memento.lastModifiedAt = member.getLastModifiedAt();
            memento.lastModifiedBy = member.getLastModifiedBy();
            memento.version = member.getVersion();
        }
    }

    /**
     * Converts this memento to a Member entity (for load operations).
     *
     * @return a Member entity reconstructed from this memento
     */
    public Member toMember() {
        // Reconstruct PersonalInformation
        PersonalInformation personalInfo = null;
        if (this.firstName != null) {
            personalInfo = PersonalInformation.of(
                    this.firstName,
                    this.lastName,
                    this.dateOfBirth,
                    this.nationality,
                    this.gender
            );
        }

        // Reconstruct Address
        Address address = null;
        if (this.street != null) {
            address = Address.of(
                    this.street,
                    this.city,
                    this.postalCode,
                    this.country
            );
        }

        // Reconstruct EmailAddress and PhoneNumber
        EmailAddress email = this.email != null ? EmailAddress.of(this.email) : null;
        PhoneNumber phone = this.phone != null ? PhoneNumber.of(this.phone) : null;

        // Reconstruct GuardianInformation
        GuardianInformation guardian = null;
        if (this.guardianFirstName != null) {
            guardian = new GuardianInformation(
                    this.guardianFirstName,
                    this.guardianLastName,
                    this.guardianRelationship,
                    EmailAddress.of(this.guardianEmail),
                    PhoneNumber.of(this.guardianPhone)
            );
        }

        // Reconstruct IdentityCard
        IdentityCard identityCard = null;
        if (this.identityCardNumber != null && this.identityCardValidityDate != null) {
            identityCard = IdentityCard.of(
                    this.identityCardNumber,
                    this.identityCardValidityDate
            );
        }

        // Reconstruct MedicalCourse
        MedicalCourse medicalCourse = null;
        if (this.medicalCourseCompletionDate != null) {
            medicalCourse = MedicalCourse.of(
                    this.medicalCourseCompletionDate,
                    Optional.ofNullable(this.medicalCourseValidityDate)
            );
        }

        // Reconstruct TrainerLicense
        TrainerLicense trainerLicense = null;
        if (this.trainerLicenseNumber != null && this.trainerLicenseValidityDate != null) {
            trainerLicense = TrainerLicense.of(
                    this.trainerLicenseNumber,
                    this.trainerLicenseValidityDate
            );
        }

        // Reconstruct RegistrationNumber
        RegistrationNumber registrationNumber = this.registrationNumber != null
                ? new RegistrationNumber(this.registrationNumber)
                : null;

        // Create Member using reconstruct method (bypasses validation)
        UserId userId = this.id != null ? new UserId(this.id) : null;
        Member member = Member.reconstruct(
                userId,
                registrationNumber,
                personalInfo,
                address,
                email,
                phone,
                guardian,
                this.active,
                this.chipNumber,
                identityCard,
                medicalCourse,
                trainerLicense,
                this.drivingLicenseGroup,
                this.dietaryRestrictions,
                getAuditMetadata()
        );

        // Store transient reference for domain event delegation
        this.member = member;

        return member;
    }

    /**
     * Returns domain events from the associated Member entity.
     * <p>
     * Annotated with @DomainEvents to enable Spring Modulith automatic event publishing.
     * Spring Data JDBC will collect and publish these events via the outbox pattern.
     *
     * @return list of domain events from the Member entity
     */
    @DomainEvents
    public List<Object> getDomainEvents() {
        if (this.member != null) {
            return this.member.getDomainEvents();
        }
        return List.of();
    }

    /**
     * Clears domain events from the associated Member entity.
     * <p>
     * Annotated with @AfterDomainEventPublication to ensure events are cleared
     * after they have been successfully published to the outbox.
     */
    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.member != null) {
            this.member.clearDomainEvents();
        }
    }

    /**
     * Check if this entity is new (not yet persisted).
     * Used by Spring Data JDBC to determine whether to perform INSERT or UPDATE.
     *
     * @return true if this is a new entity, false if already persisted
     */
    @Override
    public boolean isNew() {
        return this.isNew;
    }

    /**
     * Get the entity's unique identifier.
     *
     * @return the UUID of this entity
     */
    @Override
    public UUID getId() {
        return this.id;
    }

    /**
     * Get the audit metadata as an AuditMetadata value object.
     * <p>
     * Returns null if createdAt is null (new member not yet persisted).
     *
     * @return the audit metadata, or null if not available
     */
    public AuditMetadata getAuditMetadata() {
        if (this.createdAt == null) {
            return null;
        }
        return new AuditMetadata(
                this.createdAt,
                this.createdBy,
                this.lastModifiedAt,
                this.lastModifiedBy,
                this.version
        );
    }

}
