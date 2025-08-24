package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.hateoas.RepresentationModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Member attributes which can be updated by member himself (member can update some own attributes)    #### Required authorization - user can edit own member data   Additional validations: - either contact or at least 1 guardian needs to be entered
 */

@Schema(name = "EditMyDetailsForm", description = "Member attributes which can be updated by member himself (member can update some own attributes)    #### Required authorization - user can edit own member data   Additional validations: - either contact or at least 1 guardian needs to be entered ")
@JsonTypeName("EditMyDetailsForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
@club.klabis.members.domain.forms.AtLeastOneContactIsDefined.List({
        @club.klabis.members.domain.forms.AtLeastOneContactIsDefined(contactType = club.klabis.members.domain.Contact.Type.EMAIL, message = "At least one email contact must be provided"),
        @club.klabis.members.domain.forms.AtLeastOneContactIsDefined(contactType = club.klabis.members.domain.Contact.Type.PHONE, message = "At least one phone contact must be provided")
})
public class EditMyDetailsFormApiDto extends RepresentationModel<EditMyDetailsFormApiDto> {

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

    public EditMyDetailsFormApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public EditMyDetailsFormApiDto(String nationality, AddressApiDto address) {
        this.nationality = nationality;
        this.address = address;
    }

    public EditMyDetailsFormApiDto identityCard(IdentityCardApiDto identityCard) {
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

    public EditMyDetailsFormApiDto nationality(String nationality) {
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

    public EditMyDetailsFormApiDto address(AddressApiDto address) {
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

    public EditMyDetailsFormApiDto contact(ContactApiDto contact) {
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

    public EditMyDetailsFormApiDto guardians(List<@Valid LegalGuardianApiDto> guardians) {
        this.guardians = guardians;
        return this;
    }

    public EditMyDetailsFormApiDto addGuardiansItem(LegalGuardianApiDto guardiansItem) {
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

    public EditMyDetailsFormApiDto siCard(Integer siCard) {
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

    public EditMyDetailsFormApiDto bankAccount(String bankAccount) {
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

    public EditMyDetailsFormApiDto dietaryRestrictions(String dietaryRestrictions) {
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

    public EditMyDetailsFormApiDto drivingLicence(List<DrivingLicenceApiDto> drivingLicence) {
        this.drivingLicence = drivingLicence;
        return this;
    }

    public EditMyDetailsFormApiDto addDrivingLicenceItem(DrivingLicenceApiDto drivingLicenceItem) {
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

    public EditMyDetailsFormApiDto medicCourse(Boolean medicCourse) {
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
        EditMyDetailsFormApiDto editMyDetailsForm = (EditMyDetailsFormApiDto) o;
        return Objects.equals(this.identityCard, editMyDetailsForm.identityCard) &&
               Objects.equals(this.nationality, editMyDetailsForm.nationality) &&
               Objects.equals(this.address, editMyDetailsForm.address) &&
               Objects.equals(this.contact, editMyDetailsForm.contact) &&
               Objects.equals(this.guardians, editMyDetailsForm.guardians) &&
               Objects.equals(this.siCard, editMyDetailsForm.siCard) &&
               Objects.equals(this.bankAccount, editMyDetailsForm.bankAccount) &&
               Objects.equals(this.dietaryRestrictions, editMyDetailsForm.dietaryRestrictions) &&
               Objects.equals(this.drivingLicence, editMyDetailsForm.drivingLicence) &&
               Objects.equals(this.medicCourse, editMyDetailsForm.medicCourse);
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
                medicCourse);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class EditMyDetailsFormApiDto {\n");
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

        private EditMyDetailsFormApiDto instance;

        public Builder() {
            this(new EditMyDetailsFormApiDto());
        }

        protected Builder(EditMyDetailsFormApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(EditMyDetailsFormApiDto value) {
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

        /**
         * returns a built EditMyDetailsFormApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public EditMyDetailsFormApiDto build() {
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

