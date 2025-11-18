package club.klabis.members.infrastructure.restapi.dto;

import club.klabis.shared.config.hateoas.forms.InputOptions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.InputType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
/**
 * Data required to register new member.    #### Required authorization - requires &#x60;members:register&#x60; grant  Additional validations:  - either contact or guardian needs to be set - when nationality is different than &#x60;CZ&#x60;, &#x60;birthCertificateNumber&#x60; value will be ignored
 */

@Schema(name = "MemberRegistrationForm", description = "Data required to register new member.    #### Required authorization - requires `members:register` grant  Additional validations:  - either contact or guardian needs to be set - when nationality is different than `CZ`, `birthCertificateNumber` value will be ignored")
@JsonTypeName("MemberRegistrationForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-07-25T23:04:18.674684470+02:00[Europe/Prague]", comments = "Generator version: 7.6.0")
@RecordBuilder
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

    public static MemberRegistrationFormApiDto empty() {
        return new MemberRegistrationFormApiDto(null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

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
}
