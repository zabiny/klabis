package club.klabis.domain.members.forms;

import club.klabis.domain.members.Sex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@BirthCertificateIsDefinedForCzechia
public record EditAnotherMemberInfoByAdminForm(
        @NotBlank
        String firstName,
        @NotBlank
        String lastName,
        @NotNull
        LocalDate dateOfBirth,
        String birthCertificateNumber,
        @NotBlank
        String nationality,
        @NotNull
        Sex sex
) {

}
