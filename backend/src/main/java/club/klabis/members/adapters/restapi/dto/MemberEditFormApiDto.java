package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * Form to edit member information
 */

@Schema(name = "MemberEditForm", description = "Form to edit member information")
@JsonTypeName("MemberEditForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MemberEditFormApiDto extends RepresentationModel<MemberEditFormApiDto> {

    private IdentityCardApiDto identityCard;

    private String nationality;

    private AddressApiDto address;

    private ContactApiDto contact;

    @Valid
    private List<@Valid LegalGuardianApiDto> guardians = new ArrayList<>();

    private Integer siCard;

    private String bankAccount;

    private String dietaryRestrictions;

    @Valid
    private List<DrivingLicenceApiDto> drivingLicence = new ArrayList<>();

    private Boolean medicCourse;

    private String firstName;

    private String lastName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    private String birthCertificateNumber;

    private SexApiDto sex;

    public MemberEditFormApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MemberEditFormApiDto(String nationality, AddressApiDto address, String firstName, String lastName, LocalDate dateOfBirth, SexApiDto sex) {
        this.nationality = nationality;
        this.address = address;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.sex = sex;
    }

    public MemberEditFormApiDto identityCard(IdentityCardApiDto identityCard) {
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

    public MemberEditFormApiDto nationality(String nationality) {
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

    public MemberEditFormApiDto address(AddressApiDto address) {
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

    public MemberEditFormApiDto contact(ContactApiDto contact) {
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

    public MemberEditFormApiDto guardians(List<@Valid LegalGuardianApiDto> guardians) {
        this.guardians = guardians;
        return this;
    }

    public MemberEditFormApiDto addGuardiansItem(LegalGuardianApiDto guardiansItem) {
        if (this.guardians == null) {
            this.guardians = new ArrayList<>();
        }
        this.guardians.add(guardiansItem);
        return this;
    }

    /**
     * Get guardians
     *
     * @return guardians
     */
    @Valid
    @Schema(name = "guardians", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("guardians")
    public List<@Valid LegalGuardianApiDto> getGuardians() {
        return guardians;
    }

    public void setGuardians(List<@Valid LegalGuardianApiDto> guardians) {
        this.guardians = guardians;
    }

    public MemberEditFormApiDto siCard(Integer siCard) {
        this.siCard = siCard;
        return this;
    }

    /**
     * SI chip used by member
     *
     * @return siCard
     */

    @Schema(name = "siCard", description = "SI chip used by member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("siCard")
    public Integer getSiCard() {
        return siCard;
    }

    public void setSiCard(Integer siCard) {
        this.siCard = siCard;
    }

    public MemberEditFormApiDto bankAccount(String bankAccount) {
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

    public MemberEditFormApiDto dietaryRestrictions(String dietaryRestrictions) {
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

    public MemberEditFormApiDto drivingLicence(List<DrivingLicenceApiDto> drivingLicence) {
        this.drivingLicence = drivingLicence;
        return this;
    }

    public MemberEditFormApiDto addDrivingLicenceItem(DrivingLicenceApiDto drivingLicenceItem) {
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

    public MemberEditFormApiDto medicCourse(Boolean medicCourse) {
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

    public MemberEditFormApiDto firstName(String firstName) {
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

    public MemberEditFormApiDto lastName(String lastName) {
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

    public MemberEditFormApiDto dateOfBirth(LocalDate dateOfBirth) {
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

    public MemberEditFormApiDto birthCertificateNumber(String birthCertificateNumber) {
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

    public MemberEditFormApiDto sex(SexApiDto sex) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemberEditFormApiDto memberEditForm = (MemberEditFormApiDto) o;
        return Objects.equals(this.identityCard, memberEditForm.identityCard) &&
               Objects.equals(this.nationality, memberEditForm.nationality) &&
               Objects.equals(this.address, memberEditForm.address) &&
               Objects.equals(this.contact, memberEditForm.contact) &&
               Objects.equals(this.guardians, memberEditForm.guardians) &&
               Objects.equals(this.siCard, memberEditForm.siCard) &&
               Objects.equals(this.bankAccount, memberEditForm.bankAccount) &&
               Objects.equals(this.dietaryRestrictions, memberEditForm.dietaryRestrictions) &&
               Objects.equals(this.drivingLicence, memberEditForm.drivingLicence) &&
               Objects.equals(this.medicCourse, memberEditForm.medicCourse) &&
               Objects.equals(this.firstName, memberEditForm.firstName) &&
               Objects.equals(this.lastName, memberEditForm.lastName) &&
               Objects.equals(this.dateOfBirth, memberEditForm.dateOfBirth) &&
               Objects.equals(this.birthCertificateNumber, memberEditForm.birthCertificateNumber) &&
               Objects.equals(this.sex, memberEditForm.sex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityCard,
                nationality,
                address,
                contact,
                guardians,
                siCard,
                bankAccount,
                dietaryRestrictions,
                drivingLicence,
                medicCourse,
                firstName,
                lastName,
                dateOfBirth,
                birthCertificateNumber,
                sex);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MemberEditFormApiDto {\n");
        sb.append("    identityCard: ").append(toIndentedString(identityCard)).append("\n");
        sb.append("    nationality: ").append(toIndentedString(nationality)).append("\n");
        sb.append("    address: ").append(toIndentedString(address)).append("\n");
        sb.append("    contact: ").append(toIndentedString(contact)).append("\n");
        sb.append("    guardians: ").append(toIndentedString(guardians)).append("\n");
        sb.append("    siCard: ").append(toIndentedString(siCard)).append("\n");
        sb.append("    bankAccount: ").append(toIndentedString(bankAccount)).append("\n");
        sb.append("    dietaryRestrictions: ").append(toIndentedString(dietaryRestrictions)).append("\n");
        sb.append("    drivingLicence: ").append(toIndentedString(drivingLicence)).append("\n");
        sb.append("    medicCourse: ").append(toIndentedString(medicCourse)).append("\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
        sb.append("    birthCertificateNumber: ").append(toIndentedString(birthCertificateNumber)).append("\n");
        sb.append("    sex: ").append(toIndentedString(sex)).append("\n");
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

        private MemberEditFormApiDto instance;

        public Builder() {
            this(new MemberEditFormApiDto());
        }

        protected Builder(MemberEditFormApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MemberEditFormApiDto value) {
            this.instance.setIdentityCard(value.identityCard);
            this.instance.setNationality(value.nationality);
            this.instance.setAddress(value.address);
            this.instance.setContact(value.contact);
            this.instance.setGuardians(value.guardians);
            this.instance.setSiCard(value.siCard);
            this.instance.setBankAccount(value.bankAccount);
            this.instance.setDietaryRestrictions(value.dietaryRestrictions);
            this.instance.setDrivingLicence(value.drivingLicence);
            this.instance.setMedicCourse(value.medicCourse);
            this.instance.setFirstName(value.firstName);
            this.instance.setLastName(value.lastName);
            this.instance.setDateOfBirth(value.dateOfBirth);
            this.instance.setBirthCertificateNumber(value.birthCertificateNumber);
            this.instance.setSex(value.sex);
            return this;
        }

        public Builder identityCard(IdentityCardApiDto identityCard) {
            this.instance.identityCard(identityCard);
            return this;
        }

        public Builder nationality(String nationality) {
            this.instance.nationality(nationality);
            return this;
        }

        public Builder address(AddressApiDto address) {
            this.instance.address(address);
            return this;
        }

        public Builder contact(ContactApiDto contact) {
            this.instance.contact(contact);
            return this;
        }

        public Builder guardians(List<@Valid LegalGuardianApiDto> guardians) {
            this.instance.guardians(guardians);
            return this;
        }

        public Builder siCard(Integer siCard) {
            this.instance.siCard(siCard);
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

        public Builder firstName(String firstName) {
            this.instance.firstName(firstName);
            return this;
        }

        public Builder lastName(String lastName) {
            this.instance.lastName(lastName);
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.instance.dateOfBirth(dateOfBirth);
            return this;
        }

        public Builder birthCertificateNumber(String birthCertificateNumber) {
            this.instance.birthCertificateNumber(birthCertificateNumber);
            return this;
        }

        public Builder sex(SexApiDto sex) {
            this.instance.sex(sex);
            return this;
        }

        /**
         * returns a built MemberEditFormApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MemberEditFormApiDto build() {
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

