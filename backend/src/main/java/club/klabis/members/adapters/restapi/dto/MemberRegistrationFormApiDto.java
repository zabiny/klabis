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
 * Data required to register new member.    #### Required authorization - requires &#x60;members:register&#x60; grant  Additional validations:  - either contact or guardian needs to be set - when nationality is different than &#x60;CZ&#x60;, &#x60;birthCertificateNumber&#x60; value will be ignored
 */

@Schema(name = "MemberRegistrationForm", description = "Data required to register new member.    #### Required authorization - requires `members:register` grant  Additional validations:  - either contact or guardian needs to be set - when nationality is different than `CZ`, `birthCertificateNumber` value will be ignored")
@JsonTypeName("MemberRegistrationForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class MemberRegistrationFormApiDto extends RepresentationModel<MemberRegistrationFormApiDto> {

    private String firstName;

    private String lastName;

    private SexApiDto sex;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    private String birthCertificateNumber;

    private String nationality;

    private AddressApiDto address;

    private ContactApiDto contact;

    @Valid
    private List<@Valid LegalGuardianApiDto> guardians = new ArrayList<>();

    private Integer siCard;

    private String bankAccount;

    private String registrationNumber;

    private Integer orisId;

    public MemberRegistrationFormApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public MemberRegistrationFormApiDto(String firstName, String lastName, SexApiDto sex, LocalDate dateOfBirth, String nationality, AddressApiDto address) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.sex = sex;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.address = address;
    }

    public MemberRegistrationFormApiDto firstName(String firstName) {
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

    public MemberRegistrationFormApiDto lastName(String lastName) {
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

    public MemberRegistrationFormApiDto sex(SexApiDto sex) {
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

    public MemberRegistrationFormApiDto dateOfBirth(LocalDate dateOfBirth) {
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

    public MemberRegistrationFormApiDto birthCertificateNumber(String birthCertificateNumber) {
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

    public MemberRegistrationFormApiDto nationality(String nationality) {
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

    public MemberRegistrationFormApiDto address(AddressApiDto address) {
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

    public MemberRegistrationFormApiDto contact(ContactApiDto contact) {
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

    public MemberRegistrationFormApiDto guardians(List<@Valid LegalGuardianApiDto> guardians) {
        this.guardians = guardians;
        return this;
    }

    public MemberRegistrationFormApiDto addGuardiansItem(LegalGuardianApiDto guardiansItem) {
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

    public MemberRegistrationFormApiDto siCard(Integer siCard) {
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

    public MemberRegistrationFormApiDto bankAccount(String bankAccount) {
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

    public MemberRegistrationFormApiDto registrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
        return this;
    }

    /**
     * ORIS registration number
     *
     * @return registrationNumber
     */
    @Pattern(regexp = "^[A-Z]{3}[0-9]{4}$")
    @Schema(name = "registrationNumber", description = "ORIS registration number", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("registrationNumber")
    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public MemberRegistrationFormApiDto orisId(Integer orisId) {
        this.orisId = orisId;
        return this;
    }

    /**
     * Oris ID of registered orienteering runner
     *
     * @return orisId
     */

    @Schema(name = "orisId", description = "Oris ID of registered orienteering runner", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("orisId")
    public Integer getOrisId() {
        return orisId;
    }

    public void setOrisId(Integer orisId) {
        this.orisId = orisId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MemberRegistrationFormApiDto memberRegistrationForm = (MemberRegistrationFormApiDto) o;
        return Objects.equals(this.firstName, memberRegistrationForm.firstName) &&
               Objects.equals(this.lastName, memberRegistrationForm.lastName) &&
               Objects.equals(this.sex, memberRegistrationForm.sex) &&
               Objects.equals(this.dateOfBirth, memberRegistrationForm.dateOfBirth) &&
               Objects.equals(this.birthCertificateNumber, memberRegistrationForm.birthCertificateNumber) &&
               Objects.equals(this.nationality, memberRegistrationForm.nationality) &&
               Objects.equals(this.address, memberRegistrationForm.address) &&
               Objects.equals(this.contact, memberRegistrationForm.contact) &&
               Objects.equals(this.guardians, memberRegistrationForm.guardians) &&
               Objects.equals(this.siCard, memberRegistrationForm.siCard) &&
               Objects.equals(this.bankAccount, memberRegistrationForm.bankAccount) &&
               Objects.equals(this.registrationNumber, memberRegistrationForm.registrationNumber) &&
               Objects.equals(this.orisId, memberRegistrationForm.orisId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName,
                lastName,
                sex,
                dateOfBirth,
                birthCertificateNumber,
                nationality,
                address,
                contact,
                guardians,
                siCard,
                bankAccount,
                registrationNumber,
                orisId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class MemberRegistrationFormApiDto {\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    sex: ").append(toIndentedString(sex)).append("\n");
        sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
        sb.append("    birthCertificateNumber: ").append(toIndentedString(birthCertificateNumber)).append("\n");
        sb.append("    nationality: ").append(toIndentedString(nationality)).append("\n");
        sb.append("    address: ").append(toIndentedString(address)).append("\n");
        sb.append("    contact: ").append(toIndentedString(contact)).append("\n");
        sb.append("    guardians: ").append(toIndentedString(guardians)).append("\n");
        sb.append("    siCard: ").append(toIndentedString(siCard)).append("\n");
        sb.append("    bankAccount: ").append(toIndentedString(bankAccount)).append("\n");
        sb.append("    registrationNumber: ").append(toIndentedString(registrationNumber)).append("\n");
        sb.append("    orisId: ").append(toIndentedString(orisId)).append("\n");
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

        private MemberRegistrationFormApiDto instance;

        public Builder() {
            this(new MemberRegistrationFormApiDto());
        }

        protected Builder(MemberRegistrationFormApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(MemberRegistrationFormApiDto value) {
            this.instance.setFirstName(value.firstName);
            this.instance.setLastName(value.lastName);
            this.instance.setSex(value.sex);
            this.instance.setDateOfBirth(value.dateOfBirth);
            this.instance.setBirthCertificateNumber(value.birthCertificateNumber);
            this.instance.setNationality(value.nationality);
            this.instance.setAddress(value.address);
            this.instance.setContact(value.contact);
            this.instance.setGuardians(value.guardians);
            this.instance.setSiCard(value.siCard);
            this.instance.setBankAccount(value.bankAccount);
            this.instance.setRegistrationNumber(value.registrationNumber);
            this.instance.setOrisId(value.orisId);
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

        public Builder sex(SexApiDto sex) {
            this.instance.sex(sex);
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

        public Builder registrationNumber(String registrationNumber) {
            this.instance.registrationNumber(registrationNumber);
            return this;
        }

        public Builder orisId(Integer orisId) {
            this.instance.orisId(orisId);
            return this;
        }

        /**
         * returns a built MemberRegistrationFormApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public MemberRegistrationFormApiDto build() {
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

