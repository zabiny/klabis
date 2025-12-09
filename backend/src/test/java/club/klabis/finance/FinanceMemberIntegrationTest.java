package club.klabis.finance;

import club.klabis.finance.domain.AccountProjector;
import club.klabis.members.application.MemberRegistrationUseCase;
import club.klabis.members.domain.*;
import club.klabis.members.domain.forms.RegistrationFormBuilder;
import com.dpolach.eventsourcing.EventsRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.modulith.test.ApplicationModuleTest;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@ApplicationModuleTest(verifyAutomatically = false, extraIncludes = {"shared", "users", "members"}, mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES)
public class FinanceMemberIntegrationTest {

    @Autowired
    private MemberRegistrationUseCase testedUseCase;

    @Autowired
    private EventsRepository eventsRepository;

    @Test
    @DisplayName("Finance account should be created when new member is registered")
    void itShouldCreateFinanceAccountWhenMemberIsRegistered() {

        Member createdMember = testedUseCase.registerMember(RegistrationFormBuilder.builder()
                .firstName("Test")
                .lastName("Something")
                .address(new Address("Ulice", "Brno", "23445", "CZ"))
                .nationality("CZ")
                .sex(Sex.MALE)
                .dateOfBirth(LocalDate.of(1980, 1, 1))
                .birthCertificateNumber("12345566778")
                .registrationNumber(RegistrationNumber.ofZbmClub(LocalDate.of(1980, 1, 1), 2))
                .contact(List.of(Contact.phone("122334455", null), Contact.email("test@test.cz", null)))
                .build());

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() ->
                Assertions.assertThat(eventsRepository.project(new AccountProjector(createdMember.getId())))
                        .isPresent()
        );
    }

}
