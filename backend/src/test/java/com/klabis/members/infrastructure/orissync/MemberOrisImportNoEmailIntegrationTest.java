package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.OrisWebUrls;
import com.dpolach.api.orisclient.dto.ClubMember;
import com.dpolach.api.orisclient.dto.ClubMemberBuilder;
import com.klabis.CleanupTestData;
import com.klabis.TestApplicationConfiguration;
import com.klabis.common.email.EmailMessage;
import com.klabis.common.email.EmailService;
import com.klabis.common.email.LoggingEmailService;
import com.klabis.common.settings.OrisClubKeyPort;
import com.klabis.common.users.UserService;
import com.klabis.common.users.domain.AccountStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Covers tasks.md 5.4 / design.md D7: bringing a member in from ORIS never sends an e-mail and
 * always creates a {@code PENDING_ACTIVATION} user — even for a member who has no e-mail address
 * at all (no guardian, so the "at least one e-mail" completeness rule design.md D5 exempts import
 * from does not apply here).
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@MockitoBean(types = {OrisApiClient.class, OrisWebUrls.class})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Members on the sync engine — importing an ORIS member sends no e-mail")
class MemberOrisImportNoEmailIntegrationTest {

    @Autowired
    private OrisClubKeyPort orisClubKeyPort;

    @Autowired
    private MemberDiscoveryJob memberDiscoveryJob;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrisApiClient orisApiClient;

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(745_000);

    private int orisId;
    private String regNum;

    @BeforeEach
    void setUp() {
        orisId = ORIS_ID_SEQUENCE.incrementAndGet();
        regNum = "ZBM%04d".formatted(orisId % 10000);
        orisClubKeyPort.store("test-club-key");
    }

    @Test
    @DisplayName("a member without any e-mail address is imported with a PENDING_ACTIVATION user and no e-mail is sent")
    void importingMemberWithoutEmailSendsNoEmailAndCreatesPendingUser() {
        Optional<EmailMessage> lastEmailBefore = loggingEmailService().getLastEmailSent();

        stubClubMembers(memberWithoutEmail(orisId, regNum));
        memberDiscoveryJob.discoverNewMembers();

        assertThat(userService.findUserByUsername(regNum))
                .as("user created for the imported member")
                .isPresent()
                .get()
                .satisfies(user -> assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION));

        assertThat(loggingEmailService().getLastEmailSent())
                .as("importing a member must not trigger any e-mail (design.md D7)")
                .isEqualTo(lastEmailBefore);
    }

    private LoggingEmailService loggingEmailService() {
        assertThat(emailService).isInstanceOf(LoggingEmailService.class);
        return (LoggingEmailService) emailService;
    }

    /**
     * An adult member with neither an e-mail address nor a guardian - ORIS has no such record for
     * a minor, so completeness of the "at least one e-mail" rule is exercised at its emptiest here:
     * nothing to satisfy it from either side.
     */
    private static ClubMember memberWithoutEmail(int id, String regNum) {
        return ClubMemberBuilder.builder()
                .id(id)
                .userId(1)
                .regNum(regNum)
                .memberFrom(LocalDate.of(2019, 8, 7))
                .memberTo(null)
                .valid(true)
                .username("user" + id)
                .firstName("Petr")
                .lastName("Bezmailu")
                .email(null)
                .street("Bezejmenná 1")
                .city("Brno")
                .zip("60000")
                .country("CZ")
                .birthday(LocalDate.now().minusYears(30))
                .phone("+420601000000")
                .gender("M")
                .persNum(null)
                .nationality("CZ")
                .si("0")
                .build();
    }

    private void stubClubMembers(ClubMember clubMember) {
        when(orisApiClient.getClubUserList("test-club-key"))
                .thenReturn(new OrisApiClient.OrisResponse<>(
                        Map.of(String.valueOf(clubMember.id()), clubMember), "JSON", "OK", null, "getClubUserList"));
    }
}
