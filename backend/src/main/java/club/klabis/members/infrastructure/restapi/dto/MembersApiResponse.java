package club.klabis.members.infrastructure.restapi.dto;

import club.klabis.members.MemberId;
import club.klabis.members.domain.Member;
import club.klabis.members.infrastructure.restapi.ResponseViews;
import com.fasterxml.jackson.annotation.*;
import io.soabase.recordbuilder.core.RecordBuilder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * MemberApiDto
 */
@RecordBuilder
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonTypeName("Member")
@Relation("member")
public record MembersApiResponse(

        @JsonIgnore
        Member member,

        @NotNull
        @Schema(name = "id", description = "Unique identifier for the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("id")
        @JsonView(ResponseViews.Summary.class)
        MemberId id,

        @Valid
        @Schema(name = "userId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("userId")
        @JsonView(ResponseViews.Summary.class)
        Integer userId,

        @NotNull
        @Schema(name = "firstName", description = "First name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("firstName")
        @JsonView(ResponseViews.Summary.class)
        String firstName,

        @NotNull
        @Schema(name = "lastName", description = "Last name of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("lastName")
        @JsonView(ResponseViews.Summary.class)
        String lastName,

        @NotNull
        @Pattern(regexp = "^[A-Z]{3}[0-9]{4}$")
        @Schema(name = "registrationNumber", description = "ORIS registration number", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("registrationNumber")
        @JsonView(ResponseViews.Summary.class)
        String registrationNumber,

        @Pattern(regexp = "^[0-9]{6}/[0-9]{3,4}$")
        @Schema(name = "birthCertificateNumber", description = "Birth certificate number for Czech citizens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("birthCertificateNumber")
        @JsonView(ResponseViews.Detailed.class)
        String birthCertificateNumber,

        @Valid
        @Schema(name = "identityCard", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("identityCard")
        @JsonView(ResponseViews.Detailed.class)
        IdentityCardApiDto identityCard,

        @NotNull
        @Valid
        @Schema(name = "address", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("address")
        @JsonView(ResponseViews.Detailed.class)
        AddressApiDto address,

        @NotNull
        @Valid
        @Schema(name = "dateOfBirth", description = "Date of birth of the club member", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("dateOfBirth")
        @JsonView(ResponseViews.Detailed.class)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOfBirth,

        @Valid
        @Schema(name = "contact", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("contact")
        @JsonView(ResponseViews.Detailed.class)
        ContactApiDto contact,

        @Valid
        @Schema(name = "legalGuardians", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("legalGuardians")
        @JsonView(ResponseViews.Detailed.class)
        List<@Valid LegalGuardianApiDto> legalGuardians,

        @Schema(name = "siCard", description = "Chip number assigned to the club member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("siCard")
        @JsonView(ResponseViews.Detailed.class)
        Integer siCard,

        @NotNull
        @Pattern(regexp = "^[A-Z]{2}$")
        @Schema(name = "nationality", description = "two letter country code, ISO 3166-1 alpha-2", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("nationality")
        @JsonView(ResponseViews.Detailed.class)
        String nationality,

        @NotNull
        @Valid
        @Schema(name = "sex", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("sex")
        @JsonView(ResponseViews.Detailed.class)
        SexApiDto sex,

        @Valid
        @Schema(name = "licences", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("licences")
        @JsonView(ResponseViews.Detailed.class)
        LicencesApiDto licences,

        @Pattern(regexp = "^[A-Z]{2}[0-9]+$")
        @Schema(name = "bankAccount", description = "Bank account number of the club member IBAN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("bankAccount")
        @JsonView(ResponseViews.Detailed.class)
        String bankAccount,

        @JsonView(ResponseViews.Detailed.class)
        @Schema(name = "dietaryRestrictions", description = "Dietary restrictions of the club member", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("dietaryRestrictions")
        String dietaryRestrictions,

        @Valid
        @Schema(name = "drivingLicence", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("drivingLicence")
        @JsonView(ResponseViews.Detailed.class)
        List<DrivingLicenceApiDto> drivingLicence,

        @Schema(name = "medicCourse", description = "Whether the club member has completed the medic course", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("medicCourse")
        @JsonView(ResponseViews.Detailed.class)
        Boolean medicCourse
) {

    public MembersApiResponse {
        if (drivingLicence() == null) {
            drivingLicence = new ArrayList<>();
        }

        if (legalGuardians() == null) {
            legalGuardians = new ArrayList<>();
        }
    }
}

