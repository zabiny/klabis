/**
 * Data required to register new member.    #### Required authorization - requires &#x60;members:register&#x60; grant  Additional validations:  - either contact or guardian needs to be set - when nationality is different than &#x60;CZ&#x60;, &#x60;birthCertificateNumber&#x60; value will be ignored
 */

@Schema(name = "MemberRegistrationForm", description = "Data required to register new member.    #### Required authorization - requires `members:register` grant  Additional validations:  - either contact or guardian needs to be set - when nationality is different than `CZ`, `birthCertificateNumber` value will be ignored")
@JsonTypeName("MemberRegistrationForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
public record MemberRegistrationFormApiDto(
        @NotNull
        @Schema(name = "firstName", description = "First name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("firstName")
        String firstName,

        @NotNull
        @Schema(name = "lastName", description = "Last name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("lastName")
        String lastName,

        @NotNull
        @Valid
        @InputOptions
        @Schema(name = "sex", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("sex")
        SexApiDto sex,

        @NotNull
        @Valid
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(name = "dateOfBirth", description = "Date of birth of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("dateOfBirth")
        LocalDate dateOfBirth,

        @Pattern(regexp = "^[0-9]{6}/[0-9]{3,4}$")
        @Schema(name = "birthCertificateNumber", description = "Birth certificate number for Czech citizens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("birthCertificateNumber")
        String birthCertificateNumber,

        @NotNull
        @Pattern(regexp = "^[A-Z]{2}$")
        @Schema(name = "nationality", description = "two letter country code, ISO 3166-1 alpha-2", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("nationality")
        String nationality,

        @NotNull
        @Valid
        @Schema(name = "address", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("address")
        AddressApiDto address,

        @Valid
        @Schema(name = "contact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("contact")
        ContactApiDto contact,

        @Valid
        @InputType("LegalGuardians")
        @Schema(name = "guardians", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("guardians")
        List<@Valid LegalGuardianApiDto> guardians,

        @Schema(name = "siCard", description = "SI chip used by member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("siCard")
        Integer siCard,

        @Pattern(regexp = "^[A-Z]{2}[0-9]+$")
        @Schema(name = "bankAccount", description = "Bank account number of the club member IBAN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("bankAccount")
        String bankAccount,

        @Pattern(regexp = "^[A-Z]{3}[0-9]{4}$")
        @Schema(name = "registrationNumber", description = "ORIS registration number", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("registrationNumber")
        String registrationNumber,

        @Schema(name = "orisId", description = "Oris ID of registered orienteering runner", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("orisId")
        Integer orisId
) {

    /**
     * Compact constructor with default values
     */
    public MemberRegistrationFormApiDto {
        if (guardians == null) {
            guardians = new ArrayList<>();
        }
    }

    /**
     * Constructor with only required parameters
     */
    public MemberRegistrationFormApiDto(String firstName, String lastName, SexApiDto sex, LocalDate dateOfBirth, String nationality, AddressApiDto address) {
        this(firstName, lastName, sex, dateOfBirth, null, nationality, address, null, null, null, null, null, null);
    }

    public static class Builder {

        private String firstName;
        private String lastName;
        private SexApiDto sex;
        private LocalDate dateOfBirth;
        private String birthCertificateNumber;
        private String nationality;
        private AddressApiDto address;
        private ContactApiDto contact;
        private List<@Valid LegalGuardianApiDto> guardians;
        private Integer siCard;
        private String bankAccount;
        private String registrationNumber;
        private Integer orisId;

        public Builder() {
        }

        protected Builder copyOf(MemberRegistrationFormApiDto value) {
            this.firstName = value.firstName;
            this.lastName = value.lastName;
            this.sex = value.sex;
            this.dateOfBirth = value.dateOfBirth;
            this.birthCertificateNumber = value.birthCertificateNumber;
            this.nationality = value.nationality;
            this.address = value.address;
            this.contact = value.contact;
            this.guardians = value.guardians;
            this.siCard = value.siCard;
            this.bankAccount = value.bankAccount;
            this.registrationNumber = value.registrationNumber;
            this.orisId = value.orisId;
            return this;
        }

        public Builder firstName(String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder sex(SexApiDto sex) {
            this.sex = sex;
            return this;
        }

        public Builder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }

        public Builder birthCertificateNumber(String birthCertificateNumber) {
            this.birthCertificateNumber = birthCertificateNumber;
            return this;
        }

        public Builder nationality(String nationality) {
            this.nationality = nationality;
            return this;
        }

        public Builder address(AddressApiDto address) {
            this.address = address;
            return this;
        }

        public Builder contact(ContactApiDto contact) {
            this.contact = contact;
            return this;
        }

        public Builder guardians(List<@Valid LegalGuardianApiDto> guardians) {
            this.guardians = guardians;
            return this;
        }

        public Builder siCard(Integer siCard) {
            this.siCard = siCard;
            return this;
        }

        public Builder bankAccount(String bankAccount) {
            this.bankAccount = bankAccount;
            return this;
        }

        public Builder registrationNumber(String registrationNumber) {
            this.registrationNumber = registrationNumber;
            return this;
        }

        public Builder orisId(Integer orisId) {
            this.orisId = orisId;
            return this;
        }

        /**
         * returns a built MemberRegistrationFormApiDto instance.
         */
        public MemberRegistrationFormApiDto build() {
            return new MemberRegistrationFormApiDto(
                    firstName,
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
                    orisId
            );
        }

        @Override
        public String toString() {
            return getClass() + "=(firstName=" + firstName + ", lastName=" + lastName + ")";
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
