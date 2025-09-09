package club.klabis.members.infrastructure.restapi.dto;

import club.klabis.members.infrastructure.restapi.ResponseViews;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * MemberApiDto
 */

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonTypeName("Member")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MembersApiResponse extends RepresentationModel<MembersApiResponse> implements MembersListItemsInnerApiDto {

    @JsonView(ResponseViews.Summary.class)
    private Integer id;

    @JsonView(ResponseViews.Summary.class)
    private String firstName;

    @JsonView(ResponseViews.Summary.class)
    private String lastName;

    @JsonView(ResponseViews.Summary.class)
    private String registrationNumber;

    @JsonView(ResponseViews.Detailed.class)
    private String birthCertificateNumber;

    @JsonView(ResponseViews.Detailed.class)
    private IdentityCardApiDto identityCard;

    @JsonView(ResponseViews.Detailed.class)
    private AddressApiDto address;

    @JsonView(ResponseViews.Detailed.class)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @JsonView(ResponseViews.Detailed.class)
    private ContactApiDto contact;

    @Valid
    @JsonView(ResponseViews.Detailed.class)
    private List<@Valid LegalGuardianApiDto> legalGuardians = new ArrayList<>();

    @JsonView(ResponseViews.Detailed.class)
    private Integer siCard;

    @JsonView(ResponseViews.Detailed.class)
    private String nationality;

    @JsonView(ResponseViews.Detailed.class)
    private SexApiDto sex;

    @JsonView(ResponseViews.Detailed.class)
    private LicencesApiDto licences;

    @JsonView(ResponseViews.Detailed.class)
    private String bankAccount;

    @JsonView(ResponseViews.Detailed.class)
    private String dietaryRestrictions;

    @Valid
    @JsonView(ResponseViews.Detailed.class)
    private List<DrivingLicenceApiDto> drivingLicence = new ArrayList<>();

    @JsonView(ResponseViews.Detailed.class)
    private Boolean medicCourse;

    public MembersApiResponse() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MembersApiResponse(Integer id, String firstName, String lastName, String registrationNumber, AddressApiDto address, LocalDate dateOfBirth, String nationality, SexApiDto sex) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.registrationNumber = registrationNumber;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.sex = sex;
    }

    public MembersApiResponse id(Integer id) {
        this.id = id;
        return this;
    }

    /**
     * Unique identifier for the club member
     *
     * @return id
     */
    @NotNull
    @Schema(name = "id", description = "Unique identifier for the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MembersApiResponse firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    /**
     * First name of the club member
     *
     * @return firstName
     */
    @NotNull
    @Schema(name = "firstName", description = "First name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("firstName")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public MembersApiResponse lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    /**
     * Last name of the club member
     *
     * @return lastName
     */
    @NotNull
    @Schema(name = "lastName", description = "Last name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("lastName")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public MembersApiResponse registrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
        return this;
    }

    /**
     * ORIS registration number
     *
     * @return registrationNumber
     */
    @NotNull
    @Pattern(regexp = "^[A-Z]{3}[0-9]{4}$")
    @Schema(name = "registrationNumber", description = "ORIS registration number", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("registrationNumber")
    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public MembersApiResponse birthCertificateNumber(String birthCertificateNumber) {
        this.birthCertificateNumber = birthCertificateNumber;
        return this;
    }

    /**
     * Birth certificate number for Czech citizens
     *
     * @return birthCertificateNumber
     */
    @Pattern(regexp = "^[0-9]{6}/[0-9]{3,4}$")
    @Schema(name = "birthCertificateNumber", description = "Birth certificate number for Czech citizens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("birthCertificateNumber")
    public String getBirthCertificateNumber() {
        return birthCertificateNumber;
    }

    public void setBirthCertificateNumber(String birthCertificateNumber) {
        this.birthCertificateNumber = birthCertificateNumber;
    }

    public MembersApiResponse identityCard(IdentityCardApiDto identityCard) {
        this.identityCard = identityCard;
        return this;
    }

    /**
     * Get identityCard
     *
     * @return identityCard
     */
    @Valid
    @Schema(name = "identityCard", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("identityCard")
    public IdentityCardApiDto getIdentityCard() {
        return identityCard;
    }

    public void setIdentityCard(IdentityCardApiDto identityCard) {
        this.identityCard = identityCard;
    }

    public MembersApiResponse address(AddressApiDto address) {
        this.address = address;
        return this;
    }

    /**
     * Get address
     *
     * @return address
     */
    @NotNull
    @Valid
    @Schema(name = "address", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("address")
    public AddressApiDto getAddress() {
        return address;
    }

    public void setAddress(AddressApiDto address) {
        this.address = address;
    }

    public MembersApiResponse dateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
        return this;
    }

    /**
     * Date of birth of the club member
     *
     * @return dateOfBirth
     */
    @NotNull
    @Valid
    @Schema(name = "dateOfBirth", description = "Date of birth of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("dateOfBirth")
    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public MembersApiResponse contact(ContactApiDto contact) {
        this.contact = contact;
        return this;
    }

    /**
     * Get contact
     *
     * @return contact
     */
    @Valid
    @Schema(name = "contact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("contact")
    public ContactApiDto getContact() {
        return contact;
    }

    public void setContact(ContactApiDto contact) {
        this.contact = contact;
    }

    public MembersApiResponse legalGuardians(List<@Valid LegalGuardianApiDto> legalGuardians) {
        this.legalGuardians = legalGuardians;
        return this;
    }

    public MembersApiResponse addLegalGuardiansItem(LegalGuardianApiDto legalGuardiansItem) {
        if (this.legalGuardians == null) {
            this.legalGuardians = new ArrayList<>();
        }
        this.legalGuardians.add(legalGuardiansItem);
        return this;
    }

    /**
     * Get legalGuardians
     *
     * @return legalGuardians
     */
    @Valid
    @Schema(name = "legalGuardians", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("legalGuardians")
    public List<@Valid LegalGuardianApiDto> getLegalGuardians() {
        return legalGuardians;
    }

    public void setLegalGuardians(List<@Valid LegalGuardianApiDto> legalGuardians) {
        this.legalGuardians = legalGuardians;
    }

    public MembersApiResponse siCard(Integer siCard) {
        this.siCard = siCard;
        return this;
    }

    /**
     * Chip number assigned to the club member
     *
     * @return siCard
     */

    @Schema(name = "siCard", description = "Chip number assigned to the club member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("siCard")
    public Integer getSiCard() {
        return siCard;
    }

    public void setSiCard(Integer siCard) {
        this.siCard = siCard;
    }

    public MembersApiResponse nationality(String nationality) {
        this.nationality = nationality;
        return this;
    }

    /**
     * two letter country code, ISO 3166-1 alpha-2
     *
     * @return nationality
     */
    @NotNull
    @Pattern(regexp = "^[A-Z]{2}$")
    @Schema(name = "nationality", description = "two letter country code, ISO 3166-1 alpha-2", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("nationality")
    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public MembersApiResponse sex(SexApiDto sex) {
        this.sex = sex;
        return this;
    }

    /**
     * Get sex
     *
     * @return sex
     */
    @NotNull
    @Valid
    @Schema(name = "sex", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("sex")
    public SexApiDto getSex() {
        return sex;
    }

    public void setSex(SexApiDto sex) {
        this.sex = sex;
    }

    public MembersApiResponse licences(LicencesApiDto licences) {
        this.licences = licences;
        return this;
    }

    /**
     * Get licences
     *
     * @return licences
     */
    @Valid
    @Schema(name = "licences", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("licences")
    public LicencesApiDto getLicences() {
        return licences;
    }

    public void setLicences(LicencesApiDto licences) {
        this.licences = licences;
    }

    public MembersApiResponse bankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
        return this;
    }

    /**
     * Bank account number of the club member IBAN
     *
     * @return bankAccount
     */
    @Pattern(regexp = "^[A-Z]{2}[0-9]+$")
    @Schema(name = "bankAccount", description = "Bank account number of the club member IBAN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("bankAccount")
    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public MembersApiResponse dietaryRestrictions(String dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
        return this;
    }

    /**
     * Dietary restrictions of the club member
     *
     * @return dietaryRestrictions
     */

    @Schema(name = "dietaryRestrictions", description = "Dietary restrictions of the club member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("dietaryRestrictions")
    public String getDietaryRestrictions() {
        return dietaryRestrictions;
    }

    public void setDietaryRestrictions(String dietaryRestrictions) {
        this.dietaryRestrictions = dietaryRestrictions;
    }

    public MembersApiResponse drivingLicence(List<DrivingLicenceApiDto> drivingLicence) {
        this.drivingLicence = drivingLicence;
        return this;
    }

    public MembersApiResponse addDrivingLicenceItem(DrivingLicenceApiDto drivingLicenceItem) {
        if (this.drivingLicence == null) {
            this.drivingLicence = new ArrayList<>();
        }
        this.drivingLicence.add(drivingLicenceItem);
        return this;
    }

    /**
     * Get drivingLicence
     *
     * @return drivingLicence
     */
    @Valid
    @Schema(name = "drivingLicence", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("drivingLicence")
    public List<DrivingLicenceApiDto> getDrivingLicence() {
        return drivingLicence;
    }

    public void setDrivingLicence(List<DrivingLicenceApiDto> drivingLicence) {
        this.drivingLicence = drivingLicence;
    }

    public MembersApiResponse medicCourse(Boolean medicCourse) {
        this.medicCourse = medicCourse;
        return this;
    }

    /**
     * Whether the club member has completed the medic course
     *
     * @return medicCourse
     */

    @Schema(name = "medicCourse", description = "Whether the club member has completed the medic course", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("medicCourse")
    public Boolean isMedicCourse() {
        return medicCourse;
    }

    public void setMedicCourse(Boolean medicCourse) {
        this.medicCourse = medicCourse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MembersApiResponse member = (MembersApiResponse) o;
        return Objects.equals(this.id, member.id) &&
               Objects.equals(this.firstName, member.firstName) &&
               Objects.equals(this.lastName, member.lastName) &&
               Objects.equals(this.registrationNumber, member.registrationNumber) &&
               Objects.equals(this.birthCertificateNumber, member.birthCertificateNumber) &&
               Objects.equals(this.identityCard, member.identityCard) &&
               Objects.equals(this.address, member.address) &&
               Objects.equals(this.dateOfBirth, member.dateOfBirth) &&
               Objects.equals(this.contact, member.contact) &&
               Objects.equals(this.legalGuardians, member.legalGuardians) &&
               Objects.equals(this.siCard, member.siCard) &&
               Objects.equals(this.nationality, member.nationality) &&
               Objects.equals(this.sex, member.sex) &&
               Objects.equals(this.licences, member.licences) &&
               Objects.equals(this.bankAccount, member.bankAccount) &&
               Objects.equals(this.dietaryRestrictions, member.dietaryRestrictions) &&
               Objects.equals(this.drivingLicence, member.drivingLicence) &&
               Objects.equals(this.medicCourse, member.medicCourse);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id,
                firstName,
                lastName,
                registrationNumber,
                birthCertificateNumber,
                identityCard,
                address,
                dateOfBirth,
                contact,
                legalGuardians,
                siCard,
                nationality,
                sex,
                licences,
                bankAccount,
                dietaryRestrictions,
                drivingLicence,
                medicCourse);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MemberApiDto {\n");
        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    registrationNumber: ").append(toIndentedString(registrationNumber)).append("\n");
        sb.append("    birthCertificateNumber: ").append(toIndentedString(birthCertificateNumber)).append("\n");
        sb.append("    identityCard: ").append(toIndentedString(identityCard)).append("\n");
        sb.append("    address: ").append(toIndentedString(address)).append("\n");
        sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
        sb.append("    contact: ").append(toIndentedString(contact)).append("\n");
        sb.append("    legalGuardians: ").append(toIndentedString(legalGuardians)).append("\n");
        sb.append("    siCard: ").append(toIndentedString(siCard)).append("\n");
        sb.append("    nationality: ").append(toIndentedString(nationality)).append("\n");
        sb.append("    sex: ").append(toIndentedString(sex)).append("\n");
        sb.append("    licences: ").append(toIndentedString(licences)).append("\n");
        sb.append("    bankAccount: ").append(toIndentedString(bankAccount)).append("\n");
        sb.append("    dietaryRestrictions: ").append(toIndentedString(dietaryRestrictions)).append("\n");
        sb.append("    drivingLicence: ").append(toIndentedString(drivingLicence)).append("\n");
        sb.append("    medicCourse: ").append(toIndentedString(medicCourse)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }

    public static class Builder {

        private MembersApiResponse instance;

        public Builder() {
            this(new MembersApiResponse());
        }

        protected Builder(MembersApiResponse instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MembersApiResponse value) {
            this.instance.setId(value.id);
            this.instance.setFirstName(value.firstName);
            this.instance.setLastName(value.lastName);
            this.instance.setRegistrationNumber(value.registrationNumber);
            this.instance.setBirthCertificateNumber(value.birthCertificateNumber);
            this.instance.setIdentityCard(value.identityCard);
            this.instance.setAddress(value.address);
            this.instance.setDateOfBirth(value.dateOfBirth);
            this.instance.setContact(value.contact);
            this.instance.setLegalGuardians(value.legalGuardians);
            this.instance.setSiCard(value.siCard);
            this.instance.setNationality(value.nationality);
            this.instance.setSex(value.sex);
            this.instance.setLicences(value.licences);
            this.instance.setBankAccount(value.bankAccount);
            this.instance.setDietaryRestrictions(value.dietaryRestrictions);
            this.instance.setDrivingLicence(value.drivingLicence);
            this.instance.setMedicCourse(value.medicCourse);
            return this;
        }

        public Builder id(Integer id) {
            this.instance.id(id);
            return this;
        }

        public Builder firstName(String firstName) {
            this.instance.firstName(firstName);
            return this;
        }

        public Builder lastName(String lastName) {
            this.instance.lastName(lastName);
            return this;
        }

        public Builder registrationNumber(String registrationNumber) {
            this.instance.registrationNumber(registrationNumber);
            return this;
        }

        public Builder birthCertificateNumber(String birthCertificateNumber) {
            this.instance.birthCertificateNumber(birthCertificateNumber);
            return this;
        }

        public Builder identityCard(IdentityCardApiDto identityCard) {
            this.instance.identityCard(identityCard);
            return this;
        }

        public Builder address(AddressApiDto address) {
            this.instance.address(address);
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.instance.dateOfBirth(dateOfBirth);
            return this;
        }

        public Builder contact(ContactApiDto contact) {
            this.instance.contact(contact);
            return this;
        }

        public Builder legalGuardians(List<@Valid LegalGuardianApiDto> legalGuardians) {
            this.instance.legalGuardians(legalGuardians);
            return this;
        }

        public Builder siCard(Integer siCard) {
            this.instance.siCard(siCard);
            return this;
        }

        public Builder nationality(String nationality) {
            this.instance.nationality(nationality);
            return this;
        }

        public Builder sex(SexApiDto sex) {
            this.instance.sex(sex);
            return this;
        }

        public Builder licences(LicencesApiDto licences) {
            this.instance.licences(licences);
            return this;
        }

        public Builder bankAccount(String bankAccount) {
            this.instance.bankAccount(bankAccount);
            return this;
        }

        public Builder dietaryRestrictions(String dietaryRestrictions) {
            this.instance.dietaryRestrictions(dietaryRestrictions);
            return this;
        }

        public Builder drivingLicence(List<DrivingLicenceApiDto> drivingLicence) {
            this.instance.drivingLicence(drivingLicence);
            return this;
        }

        public Builder medicCourse(Boolean medicCourse) {
            this.instance.medicCourse(medicCourse);
            return this;
        }

        /**
         * returns a built MemberApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MembersApiResponse build() {
            try {
                return this.instance;
            } finally {
                // ensure that this.instance is not reused
                this.instance = null;
            }
        }

        @Override
        public String toString() {
            return getClass() + "=(" + instance + ")";
        }
    }

    /**
     * Create a builder with no initialized field (except for the default values).
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Create a builder with a shallow copy of this instance.
     */
    public Builder toBuilder() {
        Builder builder = new Builder();
        return builder.copyOf(this);
    }

}

