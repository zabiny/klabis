package club.klabis.members.adapters.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * LegalGuardianApiDto
 */

@JsonTypeName("LegalGuardian")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class LegalGuardianApiDto extends RepresentationModel<LegalGuardianApiDto> {

    private String firstName;

    private String lastName;

    private ContactApiDto contact;

    private String note;

    public LegalGuardianApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public LegalGuardianApiDto(String firstName, String lastName, ContactApiDto contact) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.contact = contact;
    }

    public LegalGuardianApiDto firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    /**
     * First name of the guardian
     *
     * @return firstName
     */
    @NotNull
    @Schema(name = "firstName", description = "First name of the guardian", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("firstName")
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public LegalGuardianApiDto lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    /**
     * Last name of the guardian
     *
     * @return lastName
     */
    @NotNull
    @Schema(name = "lastName", description = "Last name of the guardian", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("lastName")
    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public LegalGuardianApiDto contact(ContactApiDto contact) {
        this.contact = contact;
        return this;
    }

    /**
     * Get contact
     *
     * @return contact
     */
    @NotNull
    @Valid
    @Schema(name = "contact", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("contact")
    public ContactApiDto getContact() {
        return contact;
    }

    public void setContact(ContactApiDto contact) {
        this.contact = contact;
    }

    public LegalGuardianApiDto note(String note) {
        this.note = note;
        return this;
    }

    /**
     * Note about the guardian (matka, otec)
     *
     * @return note
     */

    @Schema(name = "note", description = "Note about the guardian (matka, otec)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("note")
    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LegalGuardianApiDto legalGuardian = (LegalGuardianApiDto) o;
        return Objects.equals(this.firstName, legalGuardian.firstName) &&
               Objects.equals(this.lastName, legalGuardian.lastName) &&
               Objects.equals(this.contact, legalGuardian.contact) &&
               Objects.equals(this.note, legalGuardian.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, contact, note);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class LegalGuardianApiDto {\n");
        sb.append("    firstName: ").append(toIndentedString(firstName)).append("\n");
        sb.append("    lastName: ").append(toIndentedString(lastName)).append("\n");
        sb.append("    contact: ").append(toIndentedString(contact)).append("\n");
        sb.append("    note: ").append(toIndentedString(note)).append("\n");
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

        private LegalGuardianApiDto instance;

        public Builder() {
            this(new LegalGuardianApiDto());
        }

        protected Builder(LegalGuardianApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(LegalGuardianApiDto value) {
            this.instance.setFirstName(value.firstName);
            this.instance.setLastName(value.lastName);
            this.instance.setContact(value.contact);
            this.instance.setNote(value.note);
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

        public Builder contact(ContactApiDto contact) {
            this.instance.contact(contact);
            return this;
        }

        public Builder note(String note) {
            this.instance.note(note);
            return this;
        }

        /**
         * returns a built LegalGuardianApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public LegalGuardianApiDto build() {
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

