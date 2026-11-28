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
import com.klabis.members.MemberId;
import com.klabis.members.application.ManagementPort;
import com.klabis.members.domain.MemberUpdateMemberBuilder;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Wiring of the ORIS sync engine into the members module: discovery creates members, a later
 * ORIS change reaches them, and a Klabis-held chip number is protected as a conflict. Field mapping
 * lives in {@link MemberProjectionMapperTest}.
 */
@SpringBootTest
@ActiveProfiles({"test", "oris"})
@MockitoBean(types = {OrisApiClient.class, OrisWebUrls.class})
@Import(TestApplicationConfiguration.class)
@CleanupTestData
@DisplayName("Members on the sync engine")
class MemberOrisSyncScenarioIntegrationTest {

    @Autowired
    private OrisClubKeyPort orisClubKeyPort;

    @Autowired
    private MemberDiscoveryJob memberDiscoveryJob;

    @Autowired
    private SynchronizationPort synchronizationPort;

    @Autowired
    private ManagementPort managementPort;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrisApiClient orisApiClient;

    private static final AtomicInteger ORIS_ID_SEQUENCE = new AtomicInteger(733_000);

    @BeforeEach
    void setUp() {
        orisClubKeyPort.store("test-club-key");
    }

    @Test
    @DisplayName("discovery brings new ORIS members in with the ORIS registration number and a PENDING_ACTIVATION user, with or without e-mail, sending no e-mail")
    void discoveryBringsNewMembersIn() {
        int withEmailId = nextOrisId();
        String withEmailRegNum = regNumOf(withEmailId);
        int withoutEmailId = nextOrisId();
        String withoutEmailRegNum = regNumOf(withoutEmailId);
        Optional<EmailMessage> lastEmailBefore = loggingEmailService().getLastEmailSent();
        stubClubMembers(
                orisClubMember(withEmailId, withEmailRegNum, "700000001"),
                memberWithoutEmail(withoutEmailId, withoutEmailRegNum));

        memberDiscoveryJob.discoverNewMembers();

        SyncRecord record = enrolledRecord(withEmailId);
        assertThat(record.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(managementPort.getMember(memberIdOf(record)).getRegistrationNumber().getValue())
                .isEqualTo(withEmailRegNum);
        assertThat(managementPort.getMember(memberIdOf(record)).isDataIncomplete()).isFalse();
        SyncRecord recordWithoutEmail = enrolledRecord(withoutEmailId);
        assertThat(recordWithoutEmail.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(managementPort.getMember(memberIdOf(recordWithoutEmail)).isDataIncomplete()).isTrue();
        assertThat(userService.findUserByAnyUsername(withEmailRegNum))
                .hasValueSatisfying(user -> assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION));
        assertThat(userService.findUserByAnyUsername(withoutEmailRegNum))
                .hasValueSatisfying(user -> assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION));
        assertThat(loggingEmailService().getLastEmailSent()).isEqualTo(lastEmailBefore);
    }

    @Test
    @DisplayName("a later ORIS change to a paired member reaches the member record on the next sync pass")
    void laterOrisChangeReachesMemberRecord() {
        int orisId = nextOrisId();
        String regNum = regNumOf(orisId);
        stubClubMembers(orisClubMember(orisId, regNum, "700000001"));
        memberDiscoveryJob.discoverNewMembers();
        SyncRecord enrolled = enrolledRecord(orisId);

        stubClubMembers(orisClubMember(orisId, regNum, "700000002"));
        SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.IN_SYNC);
        assertThat(managementPort.getMember(memberIdOf(enrolled)).getPhone().value()).isEqualTo("+420700000002");
    }

    @Test
    @DisplayName("a chip number entered in Klabis while ORIS holds none becomes a conflict, not a silent clear")
    void chipNumberEnteredInKlabisBecomesConflictWhenOrisHoldsNone() {
        int orisId = nextOrisId();
        stubClubMembers(orisClubMember(orisId, regNumOf(orisId), "700000001"));
        memberDiscoveryJob.discoverNewMembers();
        SyncRecord enrolled = enrolledRecord(orisId);
        MemberId memberId = memberIdOf(enrolled);
        managementPort.updateMember(memberId, MemberUpdateMemberBuilder
                .builder(managementPort.prefilledUpdateCommand(memberId))
                .chipNumber("998877")
                .build());

        SyncRecord afterPass = synchronizationPort.synchronizeNow(enrolled.getId(), "test-user");

        assertThat(afterPass.getStatus()).isEqualTo(SyncStatus.CONFLICT);
        assertThat(managementPort.getMember(memberId).getChipNumber()).isEqualTo("998877");
    }

    private static int nextOrisId() {
        return ORIS_ID_SEQUENCE.incrementAndGet();
    }

    private static String regNumOf(int orisId) {
        return "ZBM%04d".formatted(orisId % 10000);
    }

    private LoggingEmailService loggingEmailService() {
        assertThat(emailService).isInstanceOf(LoggingEmailService.class);
        return (LoggingEmailService) emailService;
    }

    private SyncRecord enrolledRecord(int orisId) {
        SyncedEntityReference reference = synchronizationPort.findByExternalReferences(
                        SyncEntityType.MEMBER, ExternalSystem.ORIS, List.of(String.valueOf(orisId)))
                .stream()
                .findFirst()
                .orElseThrow();
        return synchronizationPort.findByTarget(reference.target()).orElseThrow();
    }

    private static MemberId memberIdOf(SyncRecord record) {
        return new MemberId(UUID.fromString(record.getTarget().entityId()));
    }

    private static ClubMember orisClubMember(int id, String regNum, String phone) {
        return ClubMemberBuilder.builder()
                .id(id)
                .userId(1)
                .regNum(regNum)
                .memberFrom(LocalDate.of(2019, 8, 7))
                .memberTo(null)
                .valid(true)
                .username("user" + id)
                .firstName("Jan")
                .lastName("Novák")
                .email(regNum.toLowerCase() + "@example.com")
                .street("Testovací 1")
                .city("Brno")
                .zip("60000")
                .country("CZ")
                .birthday(LocalDate.of(1990, 1, 15))
                .phone(phone)
                .gender("M")
                .persNum("900115/0000")
                .nationality("CZ")
                .si("0")
                .build();
    }

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

    private void stubClubMembers(ClubMember... clubMembers) {
        Map<String, ClubMember> byId = new LinkedHashMap<>();
        for (ClubMember clubMember : clubMembers) {
            byId.put(String.valueOf(clubMember.id()), clubMember);
        }
        when(orisApiClient.getClubUserList("test-club-key"))
                .thenReturn(new OrisApiClient.OrisResponse<>(byId, "JSON", "OK", null, "getClubUserList"));
    }
}
