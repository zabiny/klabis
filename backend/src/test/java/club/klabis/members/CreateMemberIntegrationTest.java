package club.klabis.members;

import club.klabis.members.application.MemberRegistrationUseCase;
import club.klabis.members.domain.Address;
import club.klabis.members.domain.Contact;
import club.klabis.members.domain.RegistrationNumber;
import club.klabis.members.domain.Sex;
import club.klabis.members.domain.forms.RegistrationFormBuilder;
import club.klabis.users.application.ApplicationUsersRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@ApplicationModuleTest(verifyAutomatically = false, mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
public class CreateMemberIntegrationTest {

    @Autowired
    private MemberRegistrationUseCase testedUseCase;

    @Autowired
    private ApplicationUsersRepository applicationUsersRepository;

    @Test
    @DisplayName("ApplicationUser should be created when new member is registered")
    void itShouldCreateApplicationUserForCreatedMember() {

        final RegistrationNumber registrationNumber = RegistrationNumber.ofZbmClub(LocalDate.of(1980, 1, 1), 1);

        testedUseCase.registerMember(RegistrationFormBuilder.builder()
                .firstName("Test")
                .lastName("Something")
                .address(new Address("Ulice", "Brno", "23445", "CZ"))
                .nationality("CZ")
                .sex(Sex.MALE)
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .birthCertificateNumber("12345566778")
                .registrationNumber(registrationNumber)
                .contact(List.of(Contact.phone("122334455", null), Contact.email("test@test.cz", null)))
                .build());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                Assertions.assertThat(applicationUsersRepository.findByUserNameValue(registrationNumber.toRegistrationId()))
                        .isPresent()
        );
    }

}
