package club.klabis.members.infrastructure.restapi.dto;

import club.klabis.members.infrastructure.restapi.validators.ApiValidationAnnotations.BirthCertificateIsDefinedForCzechiaForApi;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.RepresentationModel;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Member attributes editable by authorized user who can change details about other members  #### Required authorization - requires &#x60;members:edit&#x60; grant  Additional validations:  - when &#x60;CZ&#x60; is selected as nationality, then &#x60;birthCertificateNumber&#x60; is required value
 */

@Schema(name = "EditAnotherMemberDetailsForm", description = "Member attributes editable by authorized user who can change details about other members  #### Required authorization - requires `members:edit` grant  Additional validations:  - when `CZ` is selected as nationality, then `birthCertificateNumber` is required value")
@JsonTypeName("EditAnotherMemberDetailsForm")
@BirthCertificateIsDefinedForCzechiaForApi
public class EditAnotherMemberDetailsFormApiDto extends RepresentationModel<EditAnotherMemberDetailsFormApiDto> {

    private String firstName;

    private String lastName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    private String birthCertificateNumber;

    private String nationality;

    private SexApiDto sex;

    public EditAnotherMemberDetailsFormApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public EditAnotherMemberDetailsFormApiDto(String firstName, String lastName, LocalDate dateOfBirth, String nationality, SexApiDto sex) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.sex = sex;
    }

    public EditAnotherMemberDetailsFormApiDto firstName(String firstName) {
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

    public EditAnotherMemberDetailsFormApiDto lastName(String lastName) {
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

    public EditAnotherMemberDetailsFormApiDto dateOfBirth(LocalDate dateOfBirth) {
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

    public EditAnotherMemberDetailsFormApiDto birthCertificateNumber(String birthCertificateNumber) {
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

    public EditAnotherMemberDetailsFormApiDto nationality(String nationality) {
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

    public EditAnotherMemberDetailsFormApiDto sex(SexApiDto sex) {
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
        EditAnotherMemberDetailsFormApiDto editAnotherMemberDetailsForm = (EditAnotherMemberDetailsFormApiDto) o;
        return Objects.equals(this.firstName, editAnotherMemberDetailsForm.firstName) &&
               Objects.equals(this.lastName, editAnotherMemberDetailsForm.lastName) &&
               Objects.equals(this.dateOfBirth, editAnotherMemberDetailsForm.dateOfBirth) &&
               Objects.equals(this.birthCertificateNumber, editAnotherMemberDetailsForm.birthCertificateNumber) &&
               Objects.equals(this.nationality, editAnotherMemberDetailsForm.nationality) &&
               Objects.equals(this.sex, editAnotherMemberDetailsForm.sex);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, dateOfBirth, birthCertificateNumber, nationality, sex);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class EditAnotherMemberDetailsFormApiDto {\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    dateOfBirth: ").append(toIndentedString(dateOfBirth)).append("\n");
        sb.append("    birthCertificateNumber: ").append(toIndentedString(birthCertificateNumber)).append("\n");
        sb.append("    nationality: ").append(toIndentedString(nationality)).append("\n");
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

        private EditAnotherMemberDetailsFormApiDto instance;

        public Builder() {
            this(new EditAnotherMemberDetailsFormApiDto());
        }

        protected Builder(EditAnotherMemberDetailsFormApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(EditAnotherMemberDetailsFormApiDto value) {
            this.instance.setFirstName(value.firstName);
            this.instance.setLastName(value.lastName);
            this.instance.setDateOfBirth(value.dateOfBirth);
            this.instance.setBirthCertificateNumber(value.birthCertificateNumber);
            this.instance.setNationality(value.nationality);
            this.instance.setSex(value.sex);
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

        public Builder nationality(String nationality) {
            this.instance.nationality(nationality);
            return this;
        }

        public Builder sex(SexApiDto sex) {
            this.instance.sex(sex);
            return this;
        }

        /**
         * returns a built EditAnotherMemberDetailsFormApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public EditAnotherMemberDetailsFormApiDto build() {
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

