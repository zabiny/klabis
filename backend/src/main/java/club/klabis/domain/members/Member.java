package club.klabis.domain.members;

import club.klabis.common.ConversionUtils;
import club.klabis.domain.members.forms.RegistrationForm;
import org.jmolecules.ddd.annotation.AggregateRoot;
import org.springframework.data.domain.AbstractAggregateRoot;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static club.klabis.common.ConversionUtils.list;

// TODO: split into ApplicationUser (with security stuff) and Member (for club Member information)
@AggregateRoot
public class Member extends AbstractAggregateRoot<Member> {
    private static int MAX_ID = 0;

    // required attributes
    // TODO: convert to value object
    private int id;
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
    private Collection<Contact> contact;
    private Collection<LegalGuardian> legalGuardians;
    private String siCard;
    private OBLicence obLicence;
    private TrainerLicence trainerLicence;
    private RefereeLicence refereeLicence;
    // TODO: maybe convert to value object
    private Integer orisId;
    // TODO: convert to value object
    private String bankAccount;

    // ApplicationUser attributes
    private String password;
    private String googleSubject;
    private String githubSubject;


    public static Member newMember(RegistrationForm registrationForm) {
        Member result = new Member();
        result.firstName = registrationForm.firstName();
        result.lastName = registrationForm.lastName();
        result.registration = registrationForm.registrationNumber();
        result.address = registrationForm.address();
        result.sex = registrationForm.sex();
        result.birthCertificateNumber = registrationForm.birthCertificateNumber();
        result.dateOfBirth = registrationForm.dateOfBirth();
        result.contact = list(registrationForm.contact());
        result.legalGuardians = list(registrationForm.guardians());
        result.siCard = registrationForm.siCard();
        result.nationality = registrationForm.nationality();
        result.orisId = registrationForm.orisId();
        result.bankAccount = registrationForm.bankAccount();
        return result;
    }

    static Member newMember(RegistrationNumber registrationNumber, String password) {
        Member result = new Member();
        result.registration = registrationNumber;
        result.password = password;
        return result;
    }

    protected Member() {
        this.password = "{noop}secret";
        this.id = ++MAX_ID;
    }

    public void linkWithGoogle(String googleSubject) {
        this.googleSubject = googleSubject;
    }

    public void linkWithGithub(String githubSubject) {
        this.githubSubject = githubSubject;
    }

    public void setObLicence(OBLicence obLicence) {
        this.obLicence = obLicence;
    }

    public void setTrainerLicence(TrainerLicenceType licenceType, LocalDate expiryDate) {
        this.trainerLicence = new TrainerLicence(licenceType, expiryDate);
    }

    public Address getAddress() {
        return address;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getGithubSubject() {
        return githubSubject;
    }

    public String getGoogleSubject() {
        return googleSubject;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPassword() {
        return password;
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

    public int getId() {
        return id;
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
}