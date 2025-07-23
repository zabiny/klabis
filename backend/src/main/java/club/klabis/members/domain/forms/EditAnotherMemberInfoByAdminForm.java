package club.klabis.members.domain.forms;

import club.klabis.members.domain.Sex;
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
