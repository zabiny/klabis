package club.klabis.members.infrastructure.restapi.dto;

import club.klabis.members.infrastructure.restapi.validators.ApiValidationAnnotations.AtLeastOneContactIsDefinedForApi;
import club.klabis.shared.config.hateoas.forms.InputOptions;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.hateoas.InputType;

import java.util.List;

import static club.klabis.shared.config.hateoas.KlabisInputTypes.CHECKBOX_INPUT_TYPE;

/**
 * Member attributes which can be updated by member himself (member can update some own attributes)    #### Required authorization - user can edit own member data   Additional validations: - either contact or at least 1 guardian needs to be entered
 */

@Schema(name = "EditMyDetailsForm", description = "Member attributes which can be updated by member himself (member can update some own attributes)    #### Required authorization - user can edit own member data   Additional validations: - either contact or at least 1 guardian needs to be entered ")
@JsonTypeName("EditMyDetailsForm")
@AtLeastOneContactIsDefinedForApi(contactType = club.klabis.members.domain.Contact.Type.EMAIL, message = "At least one email contact must be provided")
@AtLeastOneContactIsDefinedForApi(contactType = club.klabis.members.domain.Contact.Type.PHONE, message = "At least one phone contact must be provided")
@RecordBuilder
public record EditMyDetailsFormApiDto(

        @Schema(name = "identityCard", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Valid
        IdentityCardApiDto identityCard,

        @NotNull
        @Pattern(regexp = "^[A-Z]{2}$")
        @Schema(name = "nationality", description = "two letter country code, ISO 3166-1 alpha-2", requiredMode = Schema.RequiredMode.REQUIRED)
        String nationality,

        @NotNull
        @Valid
        @Schema(name = "address", requiredMode = Schema.RequiredMode.REQUIRED)
        AddressApiDto address,

        @Valid
        @Schema(name = "contact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        ContactApiDto contact,

        @Valid
        @Schema(name = "guardians", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @InputType("LegalGuardiansApiDto")
        List<@Valid LegalGuardianApiDto> guardians,

        @Schema(name = "siCard", description = "SI chip used by member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Integer siCard,

        @Schema(name = "bankAccount", description = "Bank account number of the club member IBAN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String bankAccount,

        @Schema(name = "dietaryRestrictions", description = "Dietary restrictions of the club member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String dietaryRestrictions,

        @Valid
        @Schema(name = "drivingLicence", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @InputOptions(sourceEnum = DrivingLicenceApiDto.class, inputType = CHECKBOX_INPUT_TYPE)
        List<DrivingLicenceApiDto> drivingLicence,

        @Schema(name = "medicCourse", description = "Whether the club member has completed the medic course", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean medicCourse
) {

}

