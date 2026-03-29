package com.klabis.members.infrastructure.jdbc;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.encryption.EncryptedString;
import com.klabis.common.users.UserId;
import com.klabis.members.MemberId;
import com.klabis.members.domain.*;
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

@Table("members")
class MemberMemento implements Persistable<UUID> {

    @Id
    @Column("id")
    private UUID id;

    @Column("registration_number")
    private String registrationNumber;

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

    @Column("street")
    private String street;

    @Column("city")
    private String city;

    @Column("postal_code")
    private String postalCode;

    @Column("country")
    private String country;

    @Column("email")
    private String email;

    @Column("phone")
    private String phone;

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

    @Column("is_active")
    private boolean active;

    @Column("chip_number")
    private String chipNumber;

    @Column("identity_card_number")
    private String identityCardNumber;

    @Column("identity_card_validity_date")
    private LocalDate identityCardValidityDate;

    @Column("medical_course_completion_date")
    private LocalDate medicalCourseCompletionDate;

    @Column("medical_course_validity_date")
    private LocalDate medicalCourseValidityDate;

    @Column("trainer_license_level")
    private TrainerLevel trainerLicenseLevel;

    @Column("trainer_license_validity_date")
    private LocalDate trainerLicenseValidityDate;

    @Column("referee_license_level")
    private RefereeLevel refereeLicenseLevel;

    @Column("referee_license_validity_date")
    private LocalDate refereeLicenseValidityDate;

    @Column("driving_license_group")
    private DrivingLicenseGroup drivingLicenseGroup;

    @Column("dietary_restrictions")
    private String dietaryRestrictions;

    @Column("birth_number")
    private EncryptedString birthNumber;

    @Column("bank_account_number")
    private String bankAccountNumber;

    @Column("suspension_reason")
    private DeactivationReason suspensionReason;

    @Column("suspended_at")
    private Instant suspendedAt;

    @Column("suspension_note")
    private String suspensionNote;

    @Column("suspended_by")
    private String suspendedBy;

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

    @Transient
    private Member member;

    @Transient
    private boolean isNew = true;

    protected MemberMemento() {
    }

    public static MemberMemento from(Member member) {
        MemberMemento memento = new MemberMemento();

        memento.id = member.getId() != null ? member.getId().uuid() : null;
        memento.registrationNumber = member.getRegistrationNumber() != null
                ? member.getRegistrationNumber().getValue()
                : null;

        PersonalInformation personalInfo = member.getPersonalInformation();
        if (personalInfo != null) {
            memento.firstName = personalInfo.getFirstName();
            memento.lastName = personalInfo.getLastName();
            memento.dateOfBirth = personalInfo.getDateOfBirth();
            memento.nationality = personalInfo.getNationalityCode();
            memento.gender = personalInfo.getGender();
        }

        Address address = member.getAddress();
        if (address != null) {
            memento.street = address.street();
            memento.city = address.city();
            memento.postalCode = address.postalCode();
            memento.country = address.country();
        }

        memento.email = member.getEmail() != null ? member.getEmail().value() : null;
        memento.phone = member.getPhone() != null ? member.getPhone().value() : null;

        GuardianInformation guardian = member.getGuardian();
        if (guardian != null) {
            memento.guardianFirstName = guardian.getFirstName();
            memento.guardianLastName = guardian.getLastName();
            memento.guardianRelationship = guardian.getRelationship();
            memento.guardianEmail = guardian.getEmailValue();
            memento.guardianPhone = guardian.getPhoneValue();
        }

        memento.active = member.isActive();
        memento.chipNumber = member.getChipNumber();

        IdentityCard identityCard = member.getIdentityCard();
        if (identityCard != null) {
            memento.identityCardNumber = identityCard.cardNumber();
            memento.identityCardValidityDate = identityCard.validityDate();
        }

        MedicalCourse medicalCourse = member.getMedicalCourse();
        if (medicalCourse != null) {
            memento.medicalCourseCompletionDate = medicalCourse.completionDate();
            memento.medicalCourseValidityDate = medicalCourse.validityDate().orElse(null);
        }

        TrainerLicense trainerLicense = member.getTrainerLicense();
        if (trainerLicense != null) {
            memento.trainerLicenseLevel = trainerLicense.level();
            memento.trainerLicenseValidityDate = trainerLicense.validityDate();
        }

        RefereeLicense refereeLicense = member.getRefereeLicense();
        if (refereeLicense != null) {
            memento.refereeLicenseLevel = refereeLicense.level();
            memento.refereeLicenseValidityDate = refereeLicense.validityDate();
        }

        memento.drivingLicenseGroup = member.getDrivingLicenseGroup();
        memento.dietaryRestrictions = member.getDietaryRestrictions();
        memento.birthNumber = member.getBirthNumber() != null ? EncryptedString.of(member.getBirthNumber().value()) : null;
        memento.bankAccountNumber = member.getBankAccountNumber() != null ? member.getBankAccountNumber().value() : null;
        memento.suspensionReason = member.getSuspensionReason();
        memento.suspendedAt = member.getSuspendedAt();
        memento.suspensionNote = member.getSuspensionNote();
        memento.suspendedBy = member.getSuspendedBy() != null ? member.getSuspendedBy().uuid().toString() : null;

        if (member.getAuditMetadata() != null) {
            memento.createdAt = member.getCreatedAt();
            memento.createdBy = member.getCreatedBy();
            memento.lastModifiedAt = member.getLastModifiedAt();
            memento.lastModifiedBy = member.getLastModifiedBy();
            memento.version = member.getVersion();
        }

        memento.member = member;
        memento.isNew = (member.getAuditMetadata() == null);

        return memento;
    }

    public Member toMember() {
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

        Address address = null;
        if (this.street != null) {
            address = Address.of(
                    this.street,
                    this.city,
                    this.postalCode,
                    this.country
            );
        }

        EmailAddress email = this.email != null ? EmailAddress.of(this.email) : null;
        PhoneNumber phone = this.phone != null ? PhoneNumber.of(this.phone) : null;

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

        IdentityCard identityCard = null;
        if (this.identityCardNumber != null && this.identityCardValidityDate != null) {
            identityCard = IdentityCard.of(
                    this.identityCardNumber,
                    this.identityCardValidityDate
            );
        }

        MedicalCourse medicalCourse = null;
        if (this.medicalCourseCompletionDate != null) {
            medicalCourse = MedicalCourse.of(
                    this.medicalCourseCompletionDate,
                    Optional.ofNullable(this.medicalCourseValidityDate)
            );
        }

        TrainerLicense trainerLicense = null;
        if (this.trainerLicenseLevel != null && this.trainerLicenseValidityDate != null) {
            trainerLicense = TrainerLicense.of(
                    this.trainerLicenseLevel,
                    this.trainerLicenseValidityDate
            );
        }

        RefereeLicense refereeLicense = null;
        if (this.refereeLicenseLevel != null && this.refereeLicenseValidityDate != null) {
            refereeLicense = RefereeLicense.of(
                    this.refereeLicenseLevel,
                    this.refereeLicenseValidityDate
            );
        }

        RegistrationNumber registrationNumber = this.registrationNumber != null
                ? new RegistrationNumber(this.registrationNumber)
                : null;
        BirthNumber birthNumber = this.birthNumber != null ? BirthNumber.of(this.birthNumber.value()) : null;
        BankAccountNumber bankAccountNumber = this.bankAccountNumber != null
                ? BankAccountNumber.of(this.bankAccountNumber)
                : null;

        MemberId memberId = this.id != null ? new MemberId(this.id) : null;
        Member member = Member.reconstruct(
                memberId,
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
                refereeLicense,
                this.drivingLicenseGroup,
                this.dietaryRestrictions,
                birthNumber,
                bankAccountNumber,
                this.suspensionReason,
                this.suspendedAt,
                this.suspensionNote,
                this.suspendedBy != null ? UserId.fromString(this.suspendedBy) : null,
                getAuditMetadata()
        );

        this.member = member;

        return member;
    }

    @DomainEvents
    public List<Object> getDomainEvents() {
        if (this.member != null) {
            return this.member.getDomainEvents();
        }
        return List.of();
    }

    @AfterDomainEventPublication
    public void clearDomainEvents() {
        if (this.member != null) {
            this.member.clearDomainEvents();
        }
    }

    @Override
    public boolean isNew() {
        return this.isNew;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

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
