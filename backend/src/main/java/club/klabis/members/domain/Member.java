package club.klabis.members.domain;

import club.klabis.members.MemberId;
import club.klabis.members.domain.events.MemberCreatedEvent;
import club.klabis.members.domain.events.MemberEditedEvent;
import club.klabis.members.domain.events.MembershipSuspendedEvent;
import club.klabis.members.domain.forms.EditAnotherMemberInfoByAdminForm;
import club.klabis.members.domain.forms.EditOwnMemberInfoForm;
import club.klabis.members.domain.forms.MemberEditForm;
import club.klabis.members.domain.forms.RegistrationForm;
import club.klabis.shared.domain.IncorrectFormDataException;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@AggregateRoot
public class Member extends AbstractAggregateRoot<Member> {

    private static MemberId LAST_ID = new MemberId(0);

    private static MemberId newId() {
        LAST_ID = new MemberId(LAST_ID.value() + 1);
        return LAST_ID;
    }


    // required attributes
    @Identity
    private final MemberId id;
    private String firstName;
    private String lastName;
    private RegistrationNumber registration;
    private Address address;
    private LocalDate dateOfBirth;
    private Sex sex;
    // TODO: convert to value object
    private String nationality;

    // optional attributes
    // TODO: convert to value object
    private String birthCertificateNumber;
    private IdentityCard identityCard;
    private Collection<Contact> contact = new ArrayList<>();
    private Collection<LegalGuardian> legalGuardians = new ArrayList<>();
    private String siCard;
    private OBLicence obLicence;
    private TrainerLicence trainerLicence;
    private RefereeLicence refereeLicence;
    // TODO: maybe convert to value object
    private Integer orisId;
    // TODO: convert to value object
    private String bankAccount;

    private boolean medicCourse;
    private String dietaryRestrictions;
    private List<DrivingLicence> drivingLicence = new ArrayList<>();

    // other attributes
    private boolean suspended = false;

    public static Member fromRegistration(RegistrationForm registrationForm) {

        Member result = new Member();
        result.firstName = registrationForm.firstName();
        result.lastName = registrationForm.lastName();
        result.registration = registrationForm.registrationNumber();
        result.address = registrationForm.address();
        result.sex = registrationForm.sex();
        result.birthCertificateNumber = registrationForm.birthCertificateNumber();
        result.dateOfBirth = registrationForm.dateOfBirth();
        result.contact = registrationForm.contact().stream().toList();
        result.legalGuardians = registrationForm.guardians().stream().toList();
        result.siCard = registrationForm.siCard();
        result.nationality = registrationForm.nationality();
        result.orisId = registrationForm.orisId();
        result.bankAccount = registrationForm.bankAccount();

        result.checkInvariants();

        return result.andEvent(new MemberCreatedEvent(result));
    }

    static Member fromRegistration(RegistrationNumber registrationNumber, String password) {
        Member result = new Member();
        result.registration = registrationNumber;
        result.checkInvariants();
        return result;
    }

    protected Member() {
        this.id = newId();
    }

    private void checkInvariants() {
        if (dateOfBirth != null && !registration.isValidForBirthdate(dateOfBirth)) {
            throw new IncorrectFormDataException("Registration number '%s' is not correct for birth date '%s'".formatted(registration, dateOfBirth));
        }
    }

    public void edit(MemberEditForm form) {
        this.firstName = form.firstName();
        this.lastName = form.lastName();
        this.identityCard = form.identityCard();
        this.nationality = form.nationality();
        this.address = form.address();
        this.contact.clear();
        this.contact.addAll(form.contact());
        this.legalGuardians.clear();
        this.legalGuardians.addAll(form.guardians());
        this.siCard = form.siCard();
        this.bankAccount = form.bankAccount();
        this.dietaryRestrictions = form.dietaryRestrictions();
        this.drivingLicence.clear();
        this.drivingLicence.addAll(form.drivingLicence());
        this.medicCourse = form.medicCourse();
        this.dateOfBirth = form.dateOfBirth();
        this.birthCertificateNumber = form.birthCertificateNumber();
        this.sex = form.sex();
        this.andEvent(new MemberEditedEvent(this));
    }

    public void edit(EditOwnMemberInfoForm form) {
        this.identityCard = form.identityCard();
        this.nationality = form.nationality();
        this.address = form.address();
        this.contact.clear();
        this.contact.addAll(form.contact());
        this.legalGuardians.clear();
        this.legalGuardians.addAll(form.guardians());
        this.siCard = form.siCard();
        this.bankAccount = form.bankAccount();
        this.dietaryRestrictions = form.dietaryRestrictions();
        this.drivingLicence.clear();
        this.drivingLicence.addAll(form.drivingLicence());
        this.medicCourse = form.medicCourse();
        this.andEvent(new MemberEditedEvent(this));
    }

    public void edit(EditAnotherMemberInfoByAdminForm form) {
        this.firstName = form.firstName();
        this.lastName = form.lastName();
        this.nationality = form.nationality();
        this.dateOfBirth = form.dateOfBirth();
        this.birthCertificateNumber = form.birthCertificateNumber();
        this.sex = form.sex();
        this.andEvent(new MemberEditedEvent(this));

    }

    public void suspend() {
        if (!this.suspended) {
            this.suspended = true;
            this.andEvent(new MembershipSuspendedEvent(this));
        }
    }

    public Address getAddress() {
        return address;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public RegistrationNumber getRegistration() {
        return registration;
    }

    public String getBirthCertificateNumber() {
        return birthCertificateNumber;
    }

    public Collection<Contact> getContact() {
        return contact;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Collection<LegalGuardian> getLegalGuardians() {
        return legalGuardians;
    }

    public String getNationality() {
        return nationality;
    }

    public Sex getSex() {
        return sex;
    }

    public Optional<IdentityCard> getIdentityCard() {
        return Optional.ofNullable(identityCard);
    }

    public Optional<String> getSiCard() {
        return Optional.ofNullable(siCard);
    }

    public Optional<OBLicence> getObLicence() {
        return Optional.ofNullable(obLicence);
    }

    public Optional<RefereeLicence> getRefereeLicence() {
        return Optional.ofNullable(refereeLicence);
    }

    public Optional<TrainerLicence> getTrainerLicence() {
        return Optional.ofNullable(trainerLicence);
    }

    public Optional<Integer> getOrisId() {
        return Optional.ofNullable(orisId);
    }

    public Optional<String> getBankAccount() {
        return Optional.ofNullable(bankAccount);
    }

    public MemberId getId() {
        return id;
    }

    public boolean getMedicCourse() {
        return medicCourse;
    }

    public List<DrivingLicence> getDrivingLicence() {
        return drivingLicence;
    }

    public Optional<String> getDietaryRestrictions() {
        return Optional.ofNullable(dietaryRestrictions);
    }

    public boolean isSuspended() {
        return suspended;
    }

}
