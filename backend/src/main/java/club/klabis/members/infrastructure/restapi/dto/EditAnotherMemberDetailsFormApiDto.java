package club.klabis.members.infrastructure.restapi.dto;

import club.klabis.members.infrastructure.restapi.validators.ApiValidationAnnotations.BirthCertificateIsDefinedForCzechiaForApi;
import club.klabis.shared.config.hateoas.forms.InputOptions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Member attributes editable by authorized user who can change details about other members  #### Required authorization - requires &#x60;members:edit&#x60; grant  Additional validations:  - when &#x60;CZ&#x60; is selected as nationality, then &#x60;birthCertificateNumber&#x60; is required value
 */

@Schema(name = "EditAnotherMemberDetailsForm", description = "Member attributes editable by authorized user who can change details about other members  #### Required authorization - requires `members:edit` grant  Additional validations:  - when `CZ` is selected as nationality, then `birthCertificateNumber` is required value")
@JsonTypeName("EditAnotherMemberDetailsForm")
@BirthCertificateIsDefinedForCzechiaForApi
@RecordBuilder
public record EditAnotherMemberDetailsFormApiDto(
        @NotBlank
        @Schema(name = "firstName", description = "First name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        String firstName,

        @NotBlank
        @Schema(name = "lastName", description = "Last name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        String lastName,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Schema(name = "dateOfBirth", description = "Date of birth of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate dateOfBirth,

        @Schema(name = "birthCertificateNumber", description = "Birth certificate number for Czech citizens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^[0-9]{6}/[0-9]{3,4}$")
        String birthCertificateNumber,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{2}$")
        @Schema(name = "nationality", description = "two letter country code, ISO 3166-1 alpha-2", requiredMode = Schema.RequiredMode.REQUIRED)
        String nationality,

        @NotNull
        @Schema(name = "sex", requiredMode = Schema.RequiredMode.REQUIRED)
        @InputOptions
        SexApiDto sex

) {
}

