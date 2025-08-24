package club.klabis.members.infrastructure.restapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

import java.util.Objects;

/**
 * At least one of email or phone value is required
 */

@Schema(name = "Contact", description = "At least one of email or phone value is required")
@JsonTypeName("Contact")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public class ContactApiDto extends RepresentationModel<ContactApiDto> {

    private String email;

    private String phone;

    private String note;

    public ContactApiDto() {
        super();
    }

    /**
     * Constructor with only required parameters
     */
    public ContactApiDto(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }

    public ContactApiDto email(String email) {
        this.email = email;
        return this;
    }

    /**
     * Email address of the club member or guardian
     *
     * @return email
     */
    @NotNull
    @Email
    @Schema(name = "email", description = "Email address of the club member or guardian", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public ContactApiDto phone(String phone) {
        this.phone = phone;
        return this;
    }

    /**
     * Phone number of the club member or guardian
     *
     * @return phone
     */
    @NotNull
    @Schema(name = "phone", description = "Phone number of the club member or guardian", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("phone")
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public ContactApiDto note(String note) {
        this.note = note;
        return this;
    }

    /**
     * Note about the contact
     *
     * @return note
     */

    @Schema(name = "note", description = "Note about the contact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
        ContactApiDto contact = (ContactApiDto) o;
        return Objects.equals(this.email, contact.email) &&
               Objects.equals(this.phone, contact.phone) &&
               Objects.equals(this.note, contact.note);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, phone, note);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ContactApiDto {\n");
        sb.append("    email: ").append(toIndentedString(email)).append("\n");
        sb.append("    phone: ").append(toIndentedString(phone)).append("\n");
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

        private ContactApiDto instance;

        public Builder() {
            this(new ContactApiDto());
        }

        protected Builder(ContactApiDto instance) {
            this.instance = instance;
        }

        protected Builder copyOf(ContactApiDto value) {
            this.instance.setEmail(value.email);
            this.instance.setPhone(value.phone);
            this.instance.setNote(value.note);
            return this;
        }

        public Builder email(String email) {
            this.instance.email(email);
            return this;
        }

        public Builder phone(String phone) {
            this.instance.phone(phone);
            return this;
        }

        public Builder note(String note) {
            this.instance.note(note);
            return this;
        }

        /**
         * returns a built ContactApiDto instance.
         * <p>
         * The builder is not reusable (NullPointerException)
         */
        public ContactApiDto build() {
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

