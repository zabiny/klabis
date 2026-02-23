package com.klabis.members;

import com.klabis.common.domain.AuditMetadata;
import com.klabis.common.users.UserId;
import com.klabis.members.domain.*;

import java.time.LocalDate;
import java.util.UUID;

public class MemberTestDataBuilder {

    private UUID memberId = UUID.randomUUID();
    private RegistrationNumber registrationNumber = new RegistrationNumber("ZBM1234");
    private String firstName = "Jan";
    private String lastName = "Novák";
    private LocalDate dateOfBirth = LocalDate.of(2010, 5, 15);
    private String nationality = "CZE";
    private Gender gender = Gender.MALE;
    private Address address = Address.of("Hlavní 123", "Praha", "110 00", "CZ");
    private EmailAddress email = EmailAddress.of("jan.novak@example.com");
    private PhoneNumber phone = PhoneNumber.of("+420 123 456 789");
    private GuardianInformation guardian = new GuardianInformation("Petr", "Novák", "Father",
            EmailAddress.of("petr.novak@example.com"),
            PhoneNumber.of("+420 987 654 321"));
    private boolean isActive = true;
    private String chipNumber = null;
    private IdentityCard identityCard = null;
    private MedicalCourse medicalCourse = null;
    private TrainerLicense trainerLicense = null;
    private DrivingLicenseGroup drivingLicenseGroup = null;
    private String dietaryRestrictions = null;
    private BirthNumber birthNumber = null;
    private BankAccountNumber bankAccountNumber = null;
    private AuditMetadata auditMetadata = null;

    // Termination fields
    private DeactivationReason deactivationReason = null;
    private java.time.Instant deactivatedAt = null;
    private String deactivationNote = null;
    private String deactivatedBy = null;

    private MemberTestDataBuilder() {
    }

    public static MemberTestDataBuilder aMember() {
        return new MemberTestDataBuilder();
    }

    public static MemberTestDataBuilder aMemberWithId(UUID memberId) {
        return new MemberTestDataBuilder().withId(memberId);
    }

    public MemberTestDataBuilder withId(UUID memberId) {
        this.memberId = memberId;
        return this;
    }

    public MemberTestDataBuilder withRegistrationNumber(RegistrationNumber registrationNumber) {
        this.registrationNumber = registrationNumber;
        return this;
    }

    public MemberTestDataBuilder withRegistrationNumber(String registrationNumber) {
        return withRegistrationNumber(RegistrationNumber.of(registrationNumber));
    }

    public MemberTestDataBuilder withName(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
        return this;
    }

    public MemberTestDataBuilder withFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public MemberTestDataBuilder withLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public MemberTestDataBuilder withEmail(EmailAddress email) {
        this.email = email;
        return this;
    }

    public MemberTestDataBuilder withEmail(String email) {
        return withEmail(EmailAddress.of(email));
    }

    public MemberTestDataBuilder withPhone(PhoneNumber phone) {
        this.phone = phone;
        return this;
    }

    public MemberTestDataBuilder withPhone(String phone) {
        return withPhone(PhoneNumber.of(phone));
    }

    public MemberTestDataBuilder withDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    public MemberTestDataBuilder withNationality(String nationality) {
        this.nationality = nationality;
        return this;
    }

    public MemberTestDataBuilder withGender(Gender gender) {
        this.gender = gender;
        return this;
    }

    public MemberTestDataBuilder withAddress(Address address) {
        this.address = address;
        return this;
    }

    public MemberTestDataBuilder withGuardian(GuardianInformation guardian) {
        this.guardian = guardian;
        return this;
    }

    public MemberTestDataBuilder withNoGuardian() {
        this.guardian = null;
        return this;
    }

    public MemberTestDataBuilder withActive(boolean isActive) {
        this.isActive = isActive;
        return this;
    }

    public MemberTestDataBuilder withChipNumber(String chipNumber) {
        this.chipNumber = chipNumber;
        return this;
    }

    public MemberTestDataBuilder withIdentityCard(IdentityCard identityCard) {
        this.identityCard = identityCard;
        return this;
    }

    public MemberTestDataBuilder withMedicalCourse(MedicalCourse medicalCourse) {
        this.medicalCourse = medicalCourse;
        return this;
    }

    public MemberTestDataBuilder withTrainerLicense(TrainerLicense trainerLicense) {
        this.trainerLicense = trainerLicense;
        return this;
    }

    public MemberTestDataBuilder withDrivingLicenseGroup(DrivingLicenseGroup drivingLicenseGroup) {
        this.drivingLicenseGroup = drivingLicenseGroup;
        return this;
    }

    public MemberTestDataBuilder withDietaryRestrictions(String dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
        return this;
    }

    public MemberTestDataBuilder withAuditMetadata(AuditMetadata auditMetadata) {
        this.auditMetadata = auditMetadata;
        return this;
    }

    public MemberTestDataBuilder withBirthNumber(BirthNumber birthNumber) {
        this.birthNumber = birthNumber;
        return this;
    }

    public MemberTestDataBuilder withBirthNumber(String birthNumber) {
        return withBirthNumber(BirthNumber.of(birthNumber));
    }

    public MemberTestDataBuilder withBankAccountNumber(BankAccountNumber bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
        return this;
    }

    public MemberTestDataBuilder withBankAccountNumber(String bankAccountNumber) {
        return withBankAccountNumber(BankAccountNumber.of(bankAccountNumber));
    }

    public MemberTestDataBuilder terminated(DeactivationReason reason, String note) {
        this.isActive = false;
        this.deactivationReason = reason;
        this.deactivatedAt = java.time.Instant.now();
        this.deactivationNote = note;
        this.deactivatedBy = UUID.randomUUID().toString();
        return this;
    }

    public Member build() {
        Member result = Member.reconstruct(new UserId(memberId),
                registrationNumber,
                PersonalInformation.of(firstName, lastName, dateOfBirth, nationality, gender),
                address,
                email,
                phone,
                guardian,
                isActive,
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
                deactivatedBy,
                auditMetadata
        );
        return result;
    }

    public Member.RegisterMember toRegisterMemberCommand() {
        return new Member.RegisterMember(new UserId(this.memberId),
                this.registrationNumber,
                this.build()
                        .getPersonalInformation(),
                this.address,
                this.email,
                this.phone,
                this.guardian,
                birthNumber,
                bankAccountNumber);
    }

    /**
     * Creates a default, fully populated Member domain object instance with all fields set
     *
     * @param memberId the member ID (UUID)
     * @return a Member domain object with all fields populated
     */
    public static Member defaultMemberForMapping(UUID memberId) {
        return aMemberWithId(memberId).build();
    }
}
